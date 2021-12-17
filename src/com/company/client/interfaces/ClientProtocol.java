package com.company.client.interfaces;

import com.company.SystemCodes;

public interface ClientProtocol {

    /**
     * @param username not empty username text
     * @param password password with constraints: at least 8 chars
     * @param tags content tags (max 5)
     * @return an integer (200: success, 455: failure)
     */
    public SystemCodes register(String username, String password, String[] tags);

    /**
     * @param username not empty username text
     * @param password account's password
     * @return an integer (200: success, 4:
     */
    public SystemCodes login(String username, String password);

    /**
     * @param username not empty username text
     */
    public void logout(String username);
}
