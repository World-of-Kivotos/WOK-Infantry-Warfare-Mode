package com.wok.infantry.configtransfer;

import java.util.List;

/** Only summaries and filenames travel over the network; archives stay on the server. */
public record CatalogTransferResult(boolean success, String message, String token,
                                    List<String> files, int page, int pages) {
    public CatalogTransferResult {
        files = List.copyOf(files);
    }

    public static CatalogTransferResult message(boolean success, String message, String token) {
        return new CatalogTransferResult(success, message, token, List.of(), 0, 0);
    }
}
