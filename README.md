special thanks to josé 🤠

## Key Features

- Asynchronous and synchronous message handling
- Redis pub/sub for real-time communication
- Flexible listener registration with annotation-based message routing
- Thread-safe operations for concurrent environments
- Efficient resource management with AutoCloseable implementation
- Comprehensive error handling and logging

## Core Components

- `Cacher`: Main class handling message distribution and listener management
- `Redis`: Wrapper for Redis operations, supporting both local and backbone Redis instances
- `Message`: Data structure for messages
- `MessageListener`: Interface for creating message listeners
- `IncomingMessageHandler`: Annotation for marking message handler methods
