# Caching DNS Server

## Overview
This was an assigment for my computer networks class involving a Java-based DNS server with caching and recursive query functionalities. The server handles queries not only from the local zone file but also resolves external queries by contacting another DNS server. The responses are cached to improve efficiency for future queries.

### Key Features
- Recursive query handling for names outside the local zone.
- DNS record caching with TTL (Time To Live) adherence.

## Installation and Setup
1. Clone the repository
2. Navigate to the project directory and compile the application using the provided Makefile.
3. Start the DNS server with `sudo java dns.DNSServer csci3363.zone`.

## Components
- **DNSZone.java**: Manages a single DNS zone.
- **DNSMessage.java**: Represents a DNS message.
- **DNSRecord.java**: Represents a DNS record (modified for caching).
- **DNSServer.java**: Main server class (extended for recursive queries and caching).
- **DNSCache.java**: New class for caching DNS records.

## Working with the Server
- To test, use `dig @localhost <domain-name>` in a separate terminal.
- The server handles queries by checking the local zone file first, then recursively querying an external server if needed.
- Responses are cached and used for answering future queries within their TTL.

## Acknowledgements
- Charles Wiseman