import React, { createContext, useCallback, useContext, useMemo, useState} from "react";
import { useEffect } from 'react';
import { setAuthToken, setOnUnauthorized } from '../services/apiClient';

const AuthContext = createContext(null);

export function AuthProvider({
    children
}) {
    // null - niko nije prijavljen u suprotnom { token, user}
    const [session, setSession] = useState(null);

    // auth/login
    const signIn = useCallback((loginResponse) => {
        const {token, ...user} = loginResponse;
        setAuthToken(token);
        setSession({token, user});
    }, []);

    // auth/logout
    const signOut = useCallback(() => {
        setAuthToken(null);
        setSession(null);
    }, []);
    
    useEffect(() => {
        setOnUnauthorized(signOut);
    }, [signOut]);

    const value = useMemo(() => ({
        user: session?.user ?? null,
        token: session?.token ?? null,
        role: session?.user.role ?? null,
        isSignedIn: session !== null,
        signIn,
        signOut,
        }),
        [session, signIn, signOut],
    );

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(){
    const ctx = useContext(AuthContext);
    if(!ctx){
        throw new Error('useAuth() se mora koristiti unutar <AuthProvider>');
    }
    return ctx;
}
