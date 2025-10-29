## How to Run

### Step 1: Compile
```bash
cd src
javac TCPServer.java TCPClient.java
```

### Step 2: Start the Server
Open a terminal and run:
```bash
cd src
java TCPServer
```

### Step 3: Start First Client (Alice)
Open a second terminal and run:
```bash
cd src
java TCPClient
```
Login with:
- Username: `alice`
- Password: `pass1`

### Step 4: Start Second Client (Bob)
Open a third terminal and run:
```bash
cd src
java TCPClient
```
Login with:
- Username: `bob`
- Password: `pass2`

### Step 5: Chat
- Type any message and press Enter to send to the other client
- Messages will appear automatically from the other client

### Step 6: Send a File
Type: `FILE:testfile.txt` (or any file in the src directory)

The file content will be printed to the console

## Credentials

- User 1: alice / pass1
- User 2: bob / pass2
