import { useState, useEffect, useCallback } from 'react';
import { fetchJobs as apiFetchJobs, submitIngest, retryIngest } from '../api';

export function useJobs() {
    const [jobs, setJobs] = useState([]);
    const [jobsBusy, setJobsBusy] = useState(false);

    const fetchJobs = useCallback(async () => {
        try {
            setJobs(await apiFetchJobs());
        } catch (_) { /* silent */ }
    }, []);

    useEffect(() => {
        fetchJobs();
        const t = setInterval(fetchJobs, 4000);
        return () => clearInterval(t);
    }, [fetchJobs]);

    const ingest = useCallback(async (source) => {
        setJobsBusy(true);
        try {
            await submitIngest(source);
            await fetchJobs();
        } catch (_) { /* silent */ }
        setJobsBusy(false);
    }, [fetchJobs]);

    const retryJob = useCallback(async (id) => {
        await retryIngest(id);
        fetchJobs();
    }, [fetchJobs]);

    const activeJobs = jobs.filter(j => j.status === 'RUNNING' || j.status === 'PENDING');

    return { jobs, jobsBusy, fetchJobs, ingest, retryJob, activeJobs };
}
