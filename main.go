package main

import (
	"bufio"
	"fmt"
	"os"
	"strings"
)

func main() {
	// 1. Setup the Database environment
	dirName := "bitcask_db"
	opts := &Options{syncOnPut: true, readWrite: true}

	_, err := open(dirName, opts)
	if err != nil {
		fmt.Printf("Error opening database: %v\n", err)
		return
	}

	scanner := bufio.NewScanner(os.Stdin)
	fmt.Println("Bitcask Shell v1.0")
	fmt.Println("Commands: put <key> <value> | get <key> | delete <key> | list | exit")
	fmt.Print("> ")

	// 2. Start the REPL Loop
	for scanner.Scan() {
		input := scanner.Text()
		parts := strings.Fields(input) // Splits by whitespace

		if len(parts) == 0 {
			fmt.Print("> ")
			continue
		}

		command := strings.ToLower(parts[0])

		switch command {
		case "put":
			if len(parts) < 3 {
				fmt.Println("Usage: put <key> <value>")
			} else {
				val := strings.Join(parts[2:], " ")
				resp, err := put(dirName, parts[1], val)
				if err != nil {
					fmt.Printf("Error: %v\n", err)
				} else {
					fmt.Println(resp)
				}
			}

		case "get":
			if len(parts) < 2 {
				fmt.Println("Usage: get <key>")
			} else {
				val, err := get(dirName, parts[1])
				if err != nil {
					fmt.Printf("Error: %v\n", err)
				} else {
					fmt.Println(val)
				}
			}

		case "delete":
			if len(parts) < 2 {
				fmt.Println("Usage: delete <key>")
			} else {
				resp, err := deleteKey(dirName, parts[1])
				if err != nil {
					fmt.Printf("Error: %v\n", err)
				} else {
					fmt.Println(resp)
				}
			}

		// --- NEW OPTION: LIST ---
		case "list", "list_keys":
			err := listKeys(dirName)
			if err != nil {
				fmt.Printf("Error: %v\n", err)
			}

		case "exit", "quit":
			fmt.Println("Closing Bitcask. Goodbye!")
			return

		default:
			fmt.Printf("Unknown command: %s\n", command)
		}

		fmt.Print("> ")
	}
}
