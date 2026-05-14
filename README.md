# Hotel Management Backend

Spring Boot 3 backend for hotel management, rooms, guests, and reservations.

## Requirements
- Java 17+
- Maven 3.9+

## Run
```powershell
mvn spring-boot:run
```

## Test
```powershell
mvn test
```

## H2 Console
- URL: http://localhost:8080/h2-console
- JDBC URL: jdbc:h2:mem:hoteldb
- User: sa (no password)

## Sample requests
See `samples/sample-requests.json` for example payloads.

## Endpoints
- Hotels: `GET/POST /api/hotels`, `GET/PUT/DELETE /api/hotels/{id}`
- Room types: `GET/POST /api/room-types`, `PUT/DELETE /api/room-types/{id}`
- Rooms: `GET/POST /api/rooms`, `GET/PUT/DELETE /api/rooms/{id}`, `GET /api/hotels/{hotelId}/rooms`
- Guests: `GET/POST /api/guests`, `GET/PUT/DELETE /api/guests/{id}`
- Reservations: `GET/POST /api/reservations`, `GET /api/reservations/{id}`,
  `PUT /api/reservations/{id}/confirm`, `PUT /api/reservations/{id}/cancel`
- Availability: `GET /api/reservations/availability?hotelId=&checkInDate=&checkOutDate=&guests=`

## Hotel Conversation Builder for LLM Fine-Tuning
This module lets hotel staff build structured guest-assistant conversations that can be exported
as JSON for later training or evaluation. It does not fine-tune a model or call real AI services.

### Conversation endpoints
- Conversations: `GET/POST /api/conversations`, `GET/PUT/DELETE /api/conversations/{id}`
- Messages: `POST /api/conversations/{id}/messages`, `PUT/DELETE /api/conversations/{conversationId}/messages/{messageId}`
- Exports: `GET /api/conversations/{id}/export`, `GET /api/conversations/export`
- Filters: `GET /api/conversations/category/{category}`, `GET /api/conversations/hotel/{hotelId}`,
  `GET /api/conversations/language/{language}`
- Generator: `POST /api/conversations/generate/{count}`
- Chat simulation: `POST /api/chat/simulate`

### Sample JSON payloads
Create conversation:
```json
{
  "title": "Parking question",
  "category": "PARKING",
  "language": "English",
  "description": "Guest asks about hotel parking",
  "hotelId": 1
}
```

Add message:
```json
{
  "role": "USER",
  "content": "Do you have parking?",
  "orderNumber": 2
}
```

Chat simulation:
```json
{
  "message": "Do you have breakfast?"
}
```

