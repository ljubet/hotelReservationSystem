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

