package API.Records;

public record CreateAdviceBody(String name, Long game_id, Long price) {
}

/*
{
  "name": "string",
  "game_id": 0,
  "price": 1
}
 */