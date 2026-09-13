package it.unicam.hackhub.web.dto;

public record LoginResponse(String token, String profileType, long id) {
}
