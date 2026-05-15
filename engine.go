import (
	"fmt"
	"os"
	"sync"
)

const (
    // Arbitrary threshold for file rotation
    MaxSegmentSize = 100 * 1024 * 1024 // 100 MB

    // Arbitrary limit to prevent a single huge write from breaking the buffer
    MaxRecordSize  = 1 * 1024 * 1024   // 1 MB
)

var (

	// mutex on the registery shared map 
	registryMutex sync.Mutex

	// in memory version of the directories metadata 
	registryMap   = make(map[string]*Directory)

	bootOnce      sync.Once
)

type MapValue struct {
	fileID   string
	valueSz  uint64
	valuePos uint64 // offset to start reading
	tstamp   uint64 // timestamp of last update
}

type Directory struct {
	//mutex          sync.Mutex
	inMemoryIndex  map[string]MapValue 
	activeWorkers  uint32
	activeFileId   uint32
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

// entry in the file 
type Entry struct {
  value string 
  key string 
  valueSize uint64
  keySize   uint64
  tstamp    uint64 
  crc       uint32  
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
		newFileName := fmt.Sprintf("%s/%d.data", d.directoryName, d.activeFileID)
		
		filePointer, err := os.OpenFile(newFileName, osPerm, 0666)
		if err != nil {
			return nil, fmt.Errorf("error creating active file: %w", err)
		}

		dir = &Directory{
			inMemoryIndex:  make(map[string]MapValue),
			directoryFiles: []*os.File{filePointer}, 
			activeFileId : 0 ,
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

func put(directoryName string ,string key ,string value) string , err{


	// those two functions outside the critical section 

	 entry:=constructEntry(value,key)

	 encodedEntry,recordSize,timestamp:encodeEntry(entry)

     registeryMutex.Lock()
	 defer registeryMutex.Unlock()

	 dir, exists := registryMap[directoryName]
	 
	 if !exists  {
         fmt.printf("this directory does not exist in the system")
		 return nil , fmt.Errorf("this directory is not opened ")
	 }

	 if !dir.isOpen {
        fmt.printf("this directory is not open")
		return nil , fmt.Errorf("this directory is not opened ") 
	 }

	 if !dir.readWrite {
        fmt.printf("this derectory is opened in read only mode")
		return nil , fmt.Errorf("this derectory is opened in read only mode") 
	 }

	 //append to the file

	 info, err := d.activeFile.Stat()
     if err != nil {
         return err
     }
     offset := uint64(info.Size())

	 if(offset + recordSize > MaxSegmentSize){
		//rotation logic 
		// close the active file
		d.activeFile.Sync() 
        d.activeFile.Close()



		// open new active file
        d.activeFileID++
		newFileName := fmt.Sprintf("%s/%d.data", d.directoryName, d.activeFileID)
		newFile, err := os.OpenFile(newFileName, os.O_APPEND|os.O_CREATE|os.O_RDWR, 0666)
		if err != nil {
        return fmt.Errorf("rotation failed: %w", err)
        }
		// change the pointers
		d.activeFile = newFile
		d.directoryFiles = append(d.directoryFiles, newFile)
		offset=0  
	  }

	  //append is here
	  if _, err := d.activeFile.Write(buf); err != nil {
        return err
      }

     
	  //update the index 

	 d.inMemoryIndex[key] = MapValue{
        fileID:   d.activeFile.Name(), // Use the filename as a temporary ID
        valueSz:  uint64(vsz),
        valuePos: offset + 16 + uint64(ksz), // Points directly to the Value [cite: 113]
        tstamp:   uint64(timestamp),
    }





    


}

func get()

func encodeEntry(entry *Entry) []byte , int ,uint64{
    
	 recordSize:=16 + entry.valueSize + entry.keySz    // 16(4(crc) + 4 ()+ 4() + 4() + vriablesize + variable size)
     buf:=make(byte,recordSize)

	 // calculated once and used in map (index) and in the file 

	 timestamp := uint32(time.Now().Unix())

	 
	 binary.BigEndian.PutUint32(buf[0:4], entry.checksum)
	 binary.BigEndian.PutUint32(buf[4:8], entry.timestamp )
	 binary.BigEndian.PutUint32(buf[8:12],entry.keySize )
	 binary.BigEndian.PutUint32(buf[12:16],entry.valueSize )

	 copy(buf[16:16+entry.keySize],key)
	 copy(buf[16+entry.keySize:],value)

	 return buf,recordSize,timestamp

}




func constructEntry(val string , k string ) *Entry{

	keySz:= len(k)
	valueSz:=len(val)
    checksum :=calculateChecksum()
	timestamp := uint32(time.Now().Unix())
	return &Entry {
		value :     val,
		key :       k , 
		valueSize : valueSz , 
		keySize :   keySz,
		tstamp  :timestamp,
		crc : checksum ,
	}

}
