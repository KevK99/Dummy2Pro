package me.daskabel.dummy2pro.security;

import java.io.Serializable;

public record AuthenticatedUser(Long userId, String username) implements Serializable
{
}