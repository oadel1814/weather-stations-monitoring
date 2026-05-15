package bitcask

import (
	"fmt"
	"os"
	"sync"
)

type MapValue struct {
	fileID   string
	valueSz  uint64
	valuePos uint64 // offset to start reading
	tstamp   uint64 // timestamp of last update
}

var (

	// mutex on the registery shared map
	registryMutex sync.Mutex

	// in memory version of the directories metadata
	registryMap = make(map[string]*Directory)

	bootOnce sync.Once
)

type Directory struct {
	mutex          sync.Mutex
	inMemoryIndex  map[string]MapValue
	activeWorkers  uint32
	activeFile     *os.File
	directoryFiles []*os.File
	syncOnPut      bool
	readWrite      bool
	isOpen         bool
}

type Options struct {
	syncOnPut bool
	readWrite bool
}

func open(directoryName string, options *Options) (*Directory, error) {

	// first booting all metadata
	bootOnce.Do(func() {
		bitcask_boot()
		fmt.Println("Booting Bitcask metadata...")
	})

	// in this phase all directory meta data are in memory
	registryMutex.Lock()
	defer registryMutex.Unlock()

	dir, exists := registryMap[directoryName]

	// if it is not exist so it is not opened so start creating it
	if !exists {
		osPerm := os.O_APPEND | os.O_CREATE | os.O_WRONLY
		fileName := directoryName + "_01"

		filePointer, err := os.OpenFile(fileName, osPerm, 0666)
		if err != nil {
			return nil, fmt.Errorf("error creating active file: %w", err)
		}

		dir = &Directory{
			inMemoryIndex:  make(map[string]MapValue),
			directoryFiles: []*os.File{filePointer},
			activeFile:     filePointer,
			isOpen:         true,
			syncOnPut:      options.syncOnPut,
			readWrite:      options.readWrite,
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

func put(directoryName string)
