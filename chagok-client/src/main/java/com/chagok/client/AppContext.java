package com.chagok.client;

import com.chagok.client.net.ApiClient;
import com.chagok.client.net.ClientConfig;
import com.chagok.client.net.ServerConnection;
import com.chagok.common.dto.CategoryDTO;
import com.chagok.common.dto.UserDTO;

import java.util.List;

public class AppContext {
    public final ClientConfig config;
    public final ServerConnection connection;
    public final ApiClient api;
    public UserDTO user;
    public List<CategoryDTO> categories = List.of();

    public AppContext(ClientConfig config) {
        this.config = config;
        this.connection = new ServerConnection(config);
        this.api = new ApiClient(connection);
    }
}
