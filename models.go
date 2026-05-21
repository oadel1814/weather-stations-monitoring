package main

import (
	"os"
	"sync"
)

const (
	// Arbitrary threshold for file rotation
	MaxSegmentSize = 100 * 1024 * 1024

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
	indexMutex    sync.RWMutex
	inMemoryIndex map[string]MapValue //key => metadata
	//activeWorkers  uint32
	activeFileId    uint32
	activeFile      *os.File
	currMergeFileId uint32
	directoryFiles  map[uint32]*os.File
	syncOnPut       bool
	readWrite       bool
	isOpen          bool
	directoryName   string
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
