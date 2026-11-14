package com.voltexastudios.bank;

/**
 * A bank account as surfaced to the {@code /bank accounts} listing: the
 * account id, the owning player's uuid and denormalized display name, and
 * the current balance in the smallest currency unit.
 */
public record Account(long accountId, String ownerUuid, String ownerName, long balance) {
}
