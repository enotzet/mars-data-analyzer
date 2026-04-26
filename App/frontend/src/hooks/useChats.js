import { useState, useEffect, useCallback } from 'react';
import { listChats } from '../api';

export function useChats(userId = 'default') {
    const [chats, setChats] = useState([]);

    const refresh = useCallback(async () => {
        try {
            setChats(await listChats(userId));
        } catch (_) { /* silent */ }
    }, [userId]);

    useEffect(() => { refresh(); }, [refresh]);

    return { chats, refresh };
}
