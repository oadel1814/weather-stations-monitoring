package main

import (
	"encoding/binary"
	"fmt"
	"hash/crc32"
	"os"
	"sort"
	"time"
)

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

		osPerm := os.O_APPEND | os.O_CREATE | os.O_RDWR
		os.MkdirAll(directoryName, 0755)

		dir = &Directory{
			inMemoryIndex:  make(map[string]MapValue),
			directoryFiles: make(map[uint32]*os.File),
			isOpen:         true,
			syncOnPut:      options.syncOnPut,
			readWrite:      options.readWrite,
			directoryName:  directoryName,
		}

		registryMap[directoryName] = dir

		err := dir.bistcaskBoot()
		if err != nil {
			delete(registryMap, directoryName)
			return nil, fmt.Errorf("failed to boot directory: %w", err)
		}

		newFileName := fmt.Sprintf("%s/%d.data", directoryName, dir.activeFileId)

		filePointer, err := os.OpenFile(newFileName, osPerm, 0666)
		if err != nil {
			delete(registryMap, directoryName)
			return nil, fmt.Errorf("error creating active file: %w", err)
		}

		dir.activeFile = filePointer
		dir.directoryFiles[dir.activeFileId] = filePointer

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
		return "", fmt.Errorf("directory is not existed or not opened")
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

	// registryMutex.Unlock()

}

func merge(directoryName string) error {
	registryMutex.RLock()
	dir, exists := registryMap[directoryName]
	registryMutex.RUnlock()

	if !exists || !dir.isOpen {
		return fmt.Errorf("directory not opened or does not exist")
	}

	// 1. Get and Sort File IDs to maintain chronological order
	var fileIDs []int
	dir.indexMutex.RLock()
	for id := range dir.directoryFiles {
		// We don't merge the active file
		if id != dir.activeFileId {
			fileIDs = append(fileIDs, int(id))
		}
	}
	dir.indexMutex.RUnlock()
	sort.Ints(fileIDs)

	if len(fileIDs) == 0 {
		return nil // Nothing to merge
	}

	// Track the range of files we are compacting
	firstFileClosed := uint32(fileIDs[0])
	// lastClosedFile := uint32(fileIDs[len(fileIDs)-1])

	// Temporary tracking for the new merged segments
	// start from activeFileId+1000 so merge IDs never collide with real data files
	// use a local counter so we never touch dir.currMergeFileId outside the lock
	localMergeCounter := dir.activeFileId + 1000
	currMergeFileID := localMergeCounter

	var generatedMergeIDs []uint32

	pendingIndexUpdates := make(map[string]MapValue)

	// Initialize first merge files
	mergedDataPath := fmt.Sprintf("%s/%d.data.merge", dir.directoryName, currMergeFileID)
	mergedHintPath := fmt.Sprintf("%s/%d.hint.merge", dir.directoryName, currMergeFileID)

	mFile, err := os.OpenFile(mergedDataPath, os.O_APPEND|os.O_CREATE|os.O_RDWR, 0666)
	if err != nil {
		return err
	}
	hFile, err := os.OpenFile(mergedHintPath, os.O_APPEND|os.O_CREATE|os.O_RDWR, 0666)
	if err != nil {
		return err
	}

	generatedMergeIDs = append(generatedMergeIDs, currMergeFileID)

	for _, fileID := range fileIDs {

		filePath := fmt.Sprintf("%s/%d.data", dir.directoryName, fileID)

		file, err := os.Open(filePath)
		if err != nil {
			continue
		}

		info, _ := file.Stat()
		fileSize := info.Size()
		var offset int64 = 0

		for offset < fileSize {

			// Read 16 bytes metadata
			headerBuf := make([]byte, 16)
			file.ReadAt(headerBuf, offset)

			tstmp := binary.BigEndian.Uint32(headerBuf[4:8])
			keySize := binary.BigEndian.Uint32(headerBuf[8:12])
			valueSize := binary.BigEndian.Uint32(headerBuf[12:16])
			totalRecordSize := int64(16 + keySize + valueSize)

			dataBuffer := make([]byte, keySize+valueSize)
			file.ReadAt(dataBuffer, offset+16)

			key := string(dataBuffer[0:keySize])

			// THE LIVE CHECK
			dir.indexMutex.RLock()
			indexValue, exist := dir.inMemoryIndex[key]
			dir.indexMutex.RUnlock()

			// Skip if deleted or if a newer version exists in another file
			if !exist || indexValue.tstamp > uint64(tstmp) || indexValue.fileID != uint32(fileID) || indexValue.valuePos != uint64(offset) {
				offset += totalRecordSize
				continue
			}

			// ROTATION
			mStat, _ := mFile.Stat()
			if uint64(mStat.Size())+uint64(totalRecordSize) > MaxSegmentSize {
				mFile.Sync()
				mFile.Close()
				hFile.Sync()
				hFile.Close()

				// increment local counter only, never touch dir.currMergeFileId here
				localMergeCounter++
				currMergeFileID = localMergeCounter
				generatedMergeIDs = append(generatedMergeIDs, currMergeFileID)

				mFile, _ = os.OpenFile(fmt.Sprintf("%s/%d.data.merge", dir.directoryName, currMergeFileID), os.O_APPEND|os.O_CREATE|os.O_RDWR, 0666)
				hFile, _ = os.OpenFile(fmt.Sprintf("%s/%d.hint.merge", dir.directoryName, currMergeFileID), os.O_APPEND|os.O_CREATE|os.O_RDWR, 0666)
			}

			// Write to Merge Data File
			mInfo, _ := mFile.Stat()
			writePos := mInfo.Size()

			//this is the combined record => data(dataBuffer) + metadata(headerBuf)
			combined := make([]byte, totalRecordSize)
			copy(combined[:16], headerBuf)
			copy(combined[16:], dataBuffer)
			mFile.Write(combined)

			// Write to Hint File: [tstamp][ksz][vsz][value_pos][key]
			hintHeader := make([]byte, 16+8)
			copy(hintHeader[0:16], headerBuf)
			binary.BigEndian.PutUint64(hintHeader[16:24], uint64(writePos))
			hFile.Write(append(hintHeader, []byte(key)...))
			currentFinalID := firstFileClosed + uint32(len(generatedMergeIDs)-1)
			pendingIndexUpdates[key] = MapValue{
				fileID:   currentFinalID,
				valueSz:  uint64(valueSize),
				valuePos: uint64(writePos),
				tstamp:   uint64(tstmp),
			}
			offset += totalRecordSize
		}
		file.Close()
	}

	mFile.Sync()
	mFile.Close()
	hFile.Sync()
	hFile.Close()

	// ATOMIC SWAP: Replacing old files with merged segments

	dir.indexMutex.Lock()
	defer dir.indexMutex.Unlock()

	// 1. Close and Delete originals
	for _, id := range fileIDs {
		uID := uint32(id)
		if f, exists := dir.directoryFiles[uID]; exists {
			f.Close()
		}
		os.Remove(fmt.Sprintf("%s/%d.data", dir.directoryName, uID))
		delete(dir.directoryFiles, uID)
	}

	// Promote .merge files to real .data files using your ID batching strategy
	for idx, tempID := range generatedMergeIDs {
		finalID := firstFileClosed + uint32(idx)

		oldData := fmt.Sprintf("%s/%d.data.merge", dir.directoryName, tempID)
		newData := fmt.Sprintf("%s/%d.data", dir.directoryName, finalID)
		os.Rename(oldData, newData)

		oldHint := fmt.Sprintf("%s/%d.hint.merge", dir.directoryName, tempID)
		newHint := fmt.Sprintf("%s/%d.hint", dir.directoryName, finalID)
		os.Rename(oldHint, newHint)

		// Open the newly promoted file for the directory map
		fPtr, _ := os.OpenFile(newData, os.O_RDWR, 0666)
		dir.directoryFiles[finalID] = fPtr
	}

	//  write the final counter back to the struct only here, while the lock is held
	dir.currMergeFileId = localMergeCounter

	for k, newMeta := range pendingIndexUpdates {
		// Only update if a concurrent PUT hasn't overwritten this key while we were merging
		if currentMeta, exists := dir.inMemoryIndex[k]; exists {
			if currentMeta.tstamp <= newMeta.tstamp {
				dir.inMemoryIndex[k] = newMeta
			}
		}
	}

	return nil
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
	// copy => entry key exists (search)

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

func (dir *Directory) bistcaskBoot() error {
	// We already have dir => so we can use dir.directoryName directly
	files, err := os.ReadDir(dir.directoryName)
	if err != nil {
		return nil
	}

	var fileIds []int
	for _, f := range files {
		var id int
		if _, err := fmt.Sscanf(f.Name(), "%d.data", &id); err == nil {
			fileIds = append(fileIds, id)
		}
	}

	sort.Ints(fileIds)

	for _, id := range fileIds {
		uID := uint32(id)
		hintPath := fmt.Sprintf("%s/%d.hint", dir.directoryName, id)
		dataPath := fmt.Sprintf("%s/%d.data", dir.directoryName, id)

		fPtr, err := os.OpenFile(dataPath, os.O_RDWR, 0666)
		if err != nil {
			continue
		}
		dir.directoryFiles[uID] = fPtr

		if _, err := os.Stat(hintPath); err == nil {
			//hint file exist => faster path
			dir.loadIndexFromHint(hintPath, uID)
		} else {
			//data file path
			dir.loadIndexFromData(fPtr, uID)
		}
	}

	if len(fileIds) > 0 {
		dir.activeFileId = uint32(fileIds[len(fileIds)-1]) + 1
	}

	return nil
}

func (dir *Directory) loadIndexFromHint(path string, fileID uint32) {
	hintFile, _ := os.Open(path)
	defer hintFile.Close()

	info, _ := hintFile.Stat()
	var offset int64 = 0
	for offset < info.Size() {

		buf := make([]byte, 16+8)
		hintFile.ReadAt(buf, offset)

		tstamp := binary.BigEndian.Uint32(buf[4:8])
		ksz := binary.BigEndian.Uint32(buf[8:12])
		vsz := binary.BigEndian.Uint32(buf[12:16])
		vpos := binary.BigEndian.Uint64(buf[16:24])

		keyBuf := make([]byte, ksz)
		hintFile.ReadAt(keyBuf, offset+24)
		key := string(keyBuf)

		isTombstone := false
		if vsz == uint32(len(Tombstone)) {
			valBuf := make([]byte, vsz)
			if fPtr, ok := dir.directoryFiles[fileID]; ok {
				// The value in the data file starts after the 16-byte header and the key
				fPtr.ReadAt(valBuf, int64(vpos)+16+int64(ksz))
				if string(valBuf) == Tombstone {
					isTombstone = true
				}
			}
		}

		if isTombstone {

			delete(dir.inMemoryIndex, key)
		} else {

			dir.inMemoryIndex[key] = MapValue{
				fileID:   fileID,
				valueSz:  uint64(vsz),
				valuePos: vpos,
				tstamp:   uint64(tstamp),
			}
		}

		offset += int64(24 + ksz)
	}
}

func listDirectories() {
	registryMutex.RLock()
	defer registryMutex.RUnlock()

	if len(registryMap) == 0 {
		fmt.Println("No directories found.")
		return
	}
	fmt.Println("Existing Directories:")
	for dirName := range registryMap {
		fmt.Printf("- %s\n", dirName)
	}

}

func (dir *Directory) loadIndexFromData(file *os.File, fileID uint32) {
	info, err := file.Stat()
	if err != nil {
		return
	}
	fileSize := info.Size()
	var offset int64 = 0

	for offset < fileSize {
		// Read 16-byte metadata
		headerBuf := make([]byte, 16)
		file.ReadAt(headerBuf, offset)

		tstamp := binary.BigEndian.Uint32(headerBuf[4:8])
		ksz := binary.BigEndian.Uint32(headerBuf[8:12])
		vsz := binary.BigEndian.Uint32(headerBuf[12:16])

		// Read the Key
		keyBuf := make([]byte, ksz)
		file.ReadAt(keyBuf, offset+16)
		key := string(keyBuf)

		// Read the Value
		valBuf := make([]byte, vsz)
		file.ReadAt(valBuf, offset+16+int64(ksz))
		val := string(valBuf)

		if val == Tombstone {
			// If it's a deletion => remove it from the index
			delete(dir.inMemoryIndex, key)
		} else {

			dir.inMemoryIndex[key] = MapValue{
				fileID:   fileID,
				valueSz:  uint64(vsz),
				valuePos: uint64(offset),
				tstamp:   uint64(tstamp),
			}
		}

		offset += int64(16 + ksz + vsz)
	}
}
