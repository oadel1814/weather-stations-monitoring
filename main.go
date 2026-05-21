package main

import (
	"bufio"
	"fmt"
	"os"
	"strings"
)

func main() {
	opts := &Options{syncOnPut: true, readWrite: true}
	var activeDir string // This will remember our current context

	scanner := bufio.NewScanner(os.Stdin)
	fmt.Println("=====================================")
	fmt.Println("       Bitcask Shell v2.1            ")
	fmt.Println("=====================================")
	fmt.Println("Commands:")
	fmt.Println("  dirs                  - List databases opened in this session")
	fmt.Println("  use <dir>             - Open/Switch to a database directory")
	fmt.Println("  put <key> <value>     - Insert or update a key")
	fmt.Println("  get <key>             - Retrieve a key")
	fmt.Println("  delete <key>          - Delete a key")
	fmt.Println("  list                  - List all keys in active directory")
	fmt.Println("  merge                 - Compact the active directory")
	fmt.Println("  exit                  - Close the shell")
	fmt.Println("=====================================")

	// Dynamic prompt display
	printPrompt := func() {
		if activeDir == "" {
			fmt.Print("(no-db) > ")
		} else {
			fmt.Printf("%s > ", activeDir)
		}
	}

	printPrompt()

	// REPL Loop
	for scanner.Scan() {
		input := scanner.Text()
		parts := strings.Fields(input)

		if len(parts) == 0 {
			printPrompt()
			continue
		}

		command := strings.ToLower(parts[0])

		switch command {

		case "dirs", "list_dirs":
			listDirectories()

		case "use":
			if len(parts) < 2 {
				fmt.Println("Usage: use <directory>")
			} else {
				targetDir := parts[1]
				_, err := open(targetDir, opts)
				if err != nil {
					fmt.Printf("Error opening database '%s': %v\n", targetDir, err)
				} else {
					activeDir = targetDir
					fmt.Printf("Switched to database: %s\n", activeDir)
				}
			}

		case "put":
			if activeDir == "" {
				fmt.Println("Error: No active database. Please use 'use <directory>' first.")
			} else if len(parts) < 3 {
				fmt.Println("Usage: put <key> <value>")
			} else {
				val := strings.Join(parts[2:], " ")
				resp, err := put(activeDir, parts[1], val)
				if err != nil {
					fmt.Printf("Error: %v\n", err)
				} else {
					fmt.Println(resp)
				}
			}

		case "get":
			if activeDir == "" {
				fmt.Println("Error: No active database. Please use 'use <directory>' first.")
			} else if len(parts) < 2 {
				fmt.Println("Usage: get <key>")
			} else {
				val, err := get(activeDir, parts[1])
				if err != nil {
					fmt.Printf("Error: %v\n", err)
				} else {
					fmt.Println(val)
				}
			}

		case "delete":
			if activeDir == "" {
				fmt.Println("Error: No active database. Please use 'use <directory>' first.")
			} else if len(parts) < 2 {
				fmt.Println("Usage: delete <key>")
			} else {
				resp, err := deleteKey(activeDir, parts[1])
				if err != nil {
					fmt.Printf("Error: %v\n", err)
				} else {
					fmt.Println(resp)
				}
			}

		case "list", "list_keys":
			if activeDir == "" {
				fmt.Println("Error: No active database. Please use 'use <directory>' first.")
			} else {
				err := listKeys(activeDir)
				if err != nil {
					fmt.Printf("Error: %v\n", err)
				}
			}

		case "merge":
			if activeDir == "" {
				fmt.Println("Error: No active database. Please use 'use <directory>' first.")
			} else {
				fmt.Println("Starting merge process...")
				err := merge(activeDir)
				if err != nil {
					fmt.Printf("Merge Error: %v\n", err)
				} else {
					fmt.Println("Merge completed successfully.")
				}
			}

		case "exit", "quit":
			fmt.Println("Closing Bitcask. Goodbye!")
			return

		default:
			fmt.Printf("Unknown command: %s\n", command)
		}

		printPrompt()
	}
}
