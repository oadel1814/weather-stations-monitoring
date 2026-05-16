package main

import (
	"encoding/binary"
	"fmt"
	"hash/crc32"
	"os"
	"sync"
	"time"
)

const (
	// Arbitrary threshold for file rotation
	MaxSegmentSize = 100 * 1024 * 1024 // 100 MB

	// Arbitrary limit to prevent a single huge write from breaking the buffer
	MaxRecordSize = 1 * 1024 * 1024 // 1 MB

	Tombstone = "$"
)

var (

	// mutex on the registery shared map
	// Changed to RWMutex so your .RLock() calls work
	registryMutex sync.RWMutex

	// in memory version of the directories metadata
	registryMap = make(map[string]*Directory)

	bootOnce sync.Once
)

type MapValue struct {
	fileID   uint32 // Changed to uint32 to match directoryFiles map
	valueSz  uint64
	valuePos uint64 // offset to start reading
	tstamp   uint64 // timestamp of last update
}

type Directory struct {
	indexMutex     sync.RWMutex
	inMemoryIndex  map[string]MapValue
	activeWorkers  uint32
	activeFileId   uint32
	activeFile     *os.File
	directoryFiles map[uint32]*os.File
	syncOnPut      bool
	readWrite      bool
	isOpen         bool
	directoryName  string
}

type Options struct {
	syncOnPut bool
	readWrite bool
}

// entry in the file
type Entry struct {
	value     string
	key       string
	valueSize uint32 // Sizes are 32-bit for binary encoding
	keySize   uint32
	tstamp    uint32
	crc       uint32
}

func open(directoryName string, options *Options) (*Directory, error) {

	// first booting all metadata
	bootOnce.Do(func() {
		//bitcask_boot()
		fmt.Println("Booting Bitcask metadata...")
	})

	// in this phase all directory meta data are in memory
	registryMutex.Lock()
	defer registryMutex.Unlock()

	dir, exists := registryMap[directoryName]

	// if it is not exist so it is not opened so start creating it
	if !exists {
		// Changed O_WRONLY to O_RDWR so we can read and write
		osPerm := os.O_APPEND | os.O_CREATE | os.O_RDWR
		os.MkdirAll(directoryName, 0755)
		newFileName := fmt.Sprintf("%s/%d.data", directoryName, 0)

		filePointer, err := os.OpenFile(newFileName, osPerm, 0666)
		if err != nil {
			return nil, fmt.Errorf("error creating active file: %w", err)
		}

		dir = &Directory{
			inMemoryIndex:  make(map[string]MapValue),
			directoryFiles: map[uint32]*os.File{0: filePointer},
			activeFileId:   0,
			activeFile:     filePointer,
			isOpen:         true,
			syncOnPut:      options.syncOnPut,
			readWrite:      options.readWrite,
			directoryName:  directoryName,
		}

		registryMap[directoryName] = dir
		return dir, nil
	}

	// if thread wants conflicting options block it
	if dir.readWrite != options.readWrite {
		return nil, fmt.Errorf("database already open with conflicting read/write options")
	}

	return dir, nil
}

func put(directoryName string, key string, value string) (string, error) {

	// those two functions outside the critical section

	entry := constructEntry(value, key)

	encodedEntry, recordSize, timestamp := encodeEntry(entry)

	registryMutex.RLock()
	dir, exists := registryMap[directoryName]
	registryMutex.RUnlock()

	if !exists || !dir.isOpen {
		return "", fmt.Errorf("directory is opened in read-only mode")
	}

	if !dir.readWrite {
		//fmt.printf("this derectory is opened in read only mode")
		return "", fmt.Errorf("this derectory is opened in read only mode")
	}

	//append to the file

	dir.indexMutex.Lock()
	defer dir.indexMutex.Unlock()

	info, err := dir.activeFile.Stat()
	if err != nil {
		return "", err
	}

	offset := uint64(info.Size())

	if offset+uint64(recordSize) > MaxSegmentSize {
		//rotation logic
		dir.activeFile.Sync()

		// open new active file
		dir.activeFileId++
		newFileName := fmt.Sprintf("%s/%d.data", dir.directoryName, dir.activeFileId)
		newFile, err := os.OpenFile(newFileName, os.O_APPEND|os.O_CREATE|os.O_RDWR, 0666)
		if err != nil {
			return "", fmt.Errorf("rotation failed: %w", err)
		}
		// change the pointers
		dir.activeFile = newFile
		dir.directoryFiles[dir.activeFileId] = newFile
		offset = 0
	}

	//append is here
	if _, err := dir.activeFile.Write(encodedEntry); err != nil {
		return "", err
	}

	//update the index
	dir.inMemoryIndex[key] = MapValue{
		fileID:   dir.activeFileId, // Use the active ID directly
		valueSz:  uint64(len(value)),
		valuePos: offset,
		tstamp:   uint64(timestamp),
	}

	return "OK", nil

}

func get(directoryName string, key string) (string, error) {
	registryMutex.RLock()
	dir, exists := registryMap[directoryName]
	registryMutex.RUnlock()

	if !exists || !dir.isOpen {
		return "", fmt.Errorf("directory not opened or does not exist")
	}

	//read from the index

	dir.indexMutex.RLock()
	meta, exist := dir.inMemoryIndex[key]
	if !exist {
		dir.indexMutex.RUnlock()
		return "", fmt.Errorf("this key does not exist in the index ")
	}
	targetFile, fileExists := dir.directoryFiles[meta.fileID] // o(1) search
	dir.indexMutex.RUnlock()

	if !fileExists {
		return "", fmt.Errorf("the target file does not exist  ")
	}

	// logic
	// 16 byte for headers
	bufferSize := 16 + meta.valueSz + uint64(len(key))

	byteRecord := make([]byte, bufferSize)

	_, err := targetFile.ReadAt(byteRecord, int64(meta.valuePos))

	if err != nil {
		return "", err
	}

	storedCRC := binary.BigEndian.Uint32(byteRecord[0:4])
	calculatedCRC := crc32.ChecksumIEEE(byteRecord[4:])

	if storedCRC != calculatedCRC {
		return "", fmt.Errorf("data corruption detected: checksum mismatch")
	}

	valueOffset := 16 + uint64(len(key))
	valueBytes := byteRecord[valueOffset:]

	return string(valueBytes), nil

}

func deleteKey(directoryName string, key string) (string, error) {
	// Directory Lookup
	registryMutex.RLock()
	dir, dirExist := registryMap[directoryName]
	registryMutex.RUnlock()

	if !dirExist || !dir.isOpen {
		return "", fmt.Errorf("directory not found or closed")
	}

	if !dir.readWrite {
		return "", fmt.Errorf("directory is read-only")
	}

	//  Check if key exists (Logical Check)
	dir.indexMutex.RLock()
	_, exist := dir.inMemoryIndex[key]
	dir.indexMutex.RUnlock()

	if !exist {
		return "", fmt.Errorf("key does not exist")
	}

	// Prepare the Tombstone Record
	// append "$" to represent a deletion
	entry := constructEntry(Tombstone, key)
	encodedEntry, recordSize, _ := encodeEntry(entry)

	// The Single Writer Section (Appending to Log)
	dir.indexMutex.Lock()
	defer dir.indexMutex.Unlock()

	info, err := dir.activeFile.Stat()
	if err != nil {
		return "", err
	}
	offset := uint64(info.Size())

	// Handle Rotation if the tombstone pushes us over the limit
	if offset+uint64(recordSize) > MaxSegmentSize {
		dir.activeFile.Sync()

		dir.activeFileId++

		newFileName := fmt.Sprintf("%s/%d.data", dir.directoryName, dir.activeFileId)
		newFile, err := os.OpenFile(newFileName, os.O_APPEND|os.O_CREATE|os.O_RDWR, 0666)

		if err != nil {
			return "", fmt.Errorf("rotation failed: %w", err)
		}

		dir.activeFile = newFile
		dir.directoryFiles[dir.activeFileId] = newFile
		offset = 0
	}

	// Write the tombstone to the active file
	if _, err := dir.activeFile.Write(encodedEntry); err != nil {
		return "", err
	}

	// Update the Map (Remove the key)
	delete(dir.inMemoryIndex, key)

	return "OK", nil
}

func listKeys(directoryName string) error {
	registryMutex.RLock()
	dir, exist := registryMap[directoryName]
	registryMutex.RUnlock()
	if !exist {
		return fmt.Errorf("this directory is not exist")
	}

	dir.indexMutex.RLock()
	defer dir.indexMutex.RUnlock()
	size := len(dir.inMemoryIndex)

	if size == 0 {
		fmt.Println("empty database")
		return nil
	}

	fmt.Println("Current Keys in Memory")
	for key := range dir.inMemoryIndex {
		fmt.Printf("-> %s\n", key)
	}
	fmt.Println("-------------------------------")

	return nil

}

func encodeEntry(entry *Entry) ([]byte, int, uint32) {

	recordSize := 16 + entry.keySize + entry.valueSize
	buf := make([]byte, recordSize)

	binary.BigEndian.PutUint32(buf[4:8], entry.tstamp)
	binary.BigEndian.PutUint32(buf[8:12], entry.keySize)
	binary.BigEndian.PutUint32(buf[12:16], entry.valueSize)

	copy(buf[16:16+entry.keySize], entry.key)
	copy(buf[16+entry.keySize:], entry.value)

	checksum := crc32.ChecksumIEEE(buf[4:])
	binary.BigEndian.PutUint32(buf[0:4], checksum)

	return buf, int(recordSize), entry.tstamp

}

func constructEntry(val string, k string) *Entry {

	return &Entry{
		value:     val,
		key:       k,
		valueSize: uint32(len(val)),
		keySize:   uint32(len(k)),
		tstamp:    uint32(time.Now().Unix()),
	}

}
