package com.liandrejohn.legalportal.client;

import com.liandrejohn.legalportal.user.User;

/** The client's own profile. {@code phone} is null until the client sets one. */
public record ClientProfileResponse(String firstName, String lastName, String email, String phone) {

    static ClientProfileResponse from(User user, ClientProfile profile) {
        return new ClientProfileResponse(profile.getFirstName(), profile.getLastName(), user.getEmail(),
                profile.getPhone());
    }

}
