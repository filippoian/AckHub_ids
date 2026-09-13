package it.unicam.hackhub.web.dto;

public record LoginRequest(String type, String identifier, String password) {
}
