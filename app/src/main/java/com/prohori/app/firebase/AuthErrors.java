package com.prohori.app.firebase;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

/**
 * Turns Firebase Authentication errors into short messages for the user. Wrong email and wrong
 * password give the same message on purpose, so nobody can use the app to find out which
 * emails have accounts.
 */
public final class AuthErrors {

    private AuthErrors() { }

    public static String message(Exception e) {
        if (e instanceof FirebaseAuthInvalidCredentialsException
                || e instanceof FirebaseAuthInvalidUserException) {
            return "Incorrect email or password.";
        }
        if (e instanceof FirebaseAuthUserCollisionException) {
            return "An account with this email already exists. Try signing in.";
        }
        if (e instanceof FirebaseAuthWeakPasswordException) {
            return "That password is too weak. Use at least 8 characters with a letter and a number.";
        }
        if (e instanceof FirebaseNetworkException) {
            return "No internet connection. Check your network and try again.";
        }
        if (e instanceof FirebaseTooManyRequestsException) {
            return "Too many attempts. Please wait a few minutes and try again.";
        }
        return "Something went wrong. Please try again.";
    }
}
