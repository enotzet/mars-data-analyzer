export const S = {
    /* layout */
    shell:     { display: 'flex', height: '100vh', background: '#212121', color: '#ececf1', fontFamily: "'Inter', system-ui, sans-serif" },

    /* sidebar */
    sidebar:   { width: 260, background: '#171717', display: 'flex', flexDirection: 'column', padding: '12px 10px', gap: 2, overflowY: 'auto', borderRight: '1px solid #2a2a2a' },
    newChat:   { display: 'flex', alignItems: 'center', gap: 10, width: '100%', padding: '12px 14px', background: 'transparent', border: '1px solid #333', borderRadius: 10, color: '#ececf1', cursor: 'pointer', fontSize: 14, marginBottom: 8 },
    sideSection: { display: 'flex', flexDirection: 'column', gap: 2, marginTop: 12 },
    sideLabel: { fontSize: 11, fontWeight: 600, color: '#777', textTransform: 'uppercase', letterSpacing: '.06em', padding: '4px 14px' },
    sideBtn:   { display: 'flex', alignItems: 'center', gap: 8, width: '100%', padding: '9px 14px', background: 'transparent', border: 'none', borderRadius: 8, color: '#ccc', cursor: 'pointer', fontSize: 13, textAlign: 'left', transition: 'background .12s' },
    sideBtnActive: { background: '#2a2a2a', color: '#fff' },
    badge:     { background: '#3b82f6', color: '#fff', fontSize: 10, borderRadius: 10, padding: '1px 7px', marginLeft: 'auto', fontWeight: 600 },
    dot:       (c) => ({ display: 'inline-block', width: 7, height: 7, borderRadius: '50%', background: c, flexShrink: 0 }),
    sideFooter:{ marginTop: 'auto', padding: '12px 10px', borderTop: '1px solid #2a2a2a' },
    chatTitle: { whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', fontSize: 12, flex: 1, textAlign: 'left' },
    chatEmpty: { fontSize: 11, color: '#555', padding: '6px 14px', fontStyle: 'italic' },
    chatRow:   { display: 'flex', alignItems: 'center', gap: 4, width: '100%', padding: '5px 10px', borderRadius: 8, cursor: 'pointer', background: 'transparent', border: 'none', color: '#ccc' },
    chatRowActive: { background: '#2a2a2a', color: '#fff' },
    chatIcon:  { display: 'flex', alignItems: 'center', justifyContent: 'center', width: 22, height: 22, borderRadius: 4, background: 'transparent', border: 'none', cursor: 'pointer', color: '#666', flexShrink: 0 },
    chatIconActive: { color: '#facc15' },
    ragToggle: { display: 'flex', justifyContent: 'space-between', alignItems: 'center' },
    toggleOn:  { width: 36, height: 20, borderRadius: 10, background: '#22c55e', border: 'none', cursor: 'pointer', position: 'relative', transition: 'background .15s' },
    toggleOff: { width: 36, height: 20, borderRadius: 10, background: '#555', border: 'none', cursor: 'pointer', position: 'relative', transition: 'background .15s' },
    toggleKnob:{ display: 'block', width: 16, height: 16, borderRadius: '50%', background: '#fff', transition: 'margin .15s', marginTop: 2 },

    /* main */
    main:      { flex: 1, display: 'flex', flexDirection: 'column', position: 'relative', minWidth: 0 },
    topbar:    { height: 48, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 12, borderBottom: '1px solid #2a2a2a', flexShrink: 0, fontSize: 14 },

    /* chat */
    chatScroll:{ flex: 1, overflowY: 'auto', paddingBottom: 140 },
    empty:     { display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', textAlign: 'center', padding: 24, color: '#aaa' },
    emptyIcon: { width: 64, height: 64, borderRadius: 16, background: '#2a2a2a', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#888', marginBottom: 4 },

    msgRow:    { padding: '24px 0' },
    msgInner:  { maxWidth: 768, margin: '0 auto', padding: '0 24px', display: 'flex', gap: 16 },
    avatarUser:{ width: 30, height: 30, borderRadius: '50%', background: '#fff', color: '#000', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 13, fontWeight: 600, flexShrink: 0 },
    avatarBot: { width: 30, height: 30, borderRadius: '50%', background: '#10a37f', color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 13, fontWeight: 600, flexShrink: 0 },
    msgText:   { whiteSpace: 'pre-wrap', lineHeight: 1.65, fontSize: 15 },
    provenance:{ fontSize: 10, color: '#555', marginTop: 8 },

    /* thinking dots */
    thinkDots: { display: 'flex', gap: 4, alignItems: 'center', height: 24 },

    /* sources */
    srcToggle: { display: 'flex', alignItems: 'center', gap: 6, background: 'none', border: '1px solid #333', borderRadius: 8, padding: '5px 12px', color: '#aaa', cursor: 'pointer', fontSize: 12 },
    srcList:   { display: 'flex', flexDirection: 'column', gap: 6, marginTop: 8 },
    srcCard:   { background: '#1a1a1a', borderRadius: 10, padding: '10px 14px', border: '1px solid #2a2a2a' },
    srcHeader: { display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 },
    srcBadge:  { width: 20, height: 20, borderRadius: '50%', background: '#3b82f6', color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 11, fontWeight: 600, flexShrink: 0 },
    srcName:   { fontSize: 13, fontWeight: 500, color: '#ccc' },
    srcScore:  { marginLeft: 'auto', fontSize: 11, color: '#888' },
    srcSnippet:{ fontSize: 12, color: '#999', lineHeight: 1.45 },
    srcLink:   { display: 'inline-flex', alignItems: 'center', gap: 4, fontSize: 12, color: '#60a5fa', textDecoration: 'none', marginTop: 6 },

    /* input */
    inputWrap: { position: 'absolute', bottom: 0, left: 0, right: 0, display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '12px 24px 20px', background: 'linear-gradient(transparent, #212121 20%)' },
    inputBar:  { display: 'flex', alignItems: 'flex-end', gap: 8, width: '100%', maxWidth: 768, background: '#303030', borderRadius: 16, border: '1px solid #444', padding: '8px 12px 8px 18px' },
    textarea:  { flex: 1, background: 'transparent', border: 'none', color: '#fff', fontSize: 15, outline: 'none', resize: 'none', lineHeight: 1.5, maxHeight: 160, fontFamily: 'inherit' },
    sendBtn:   { width: 34, height: 34, borderRadius: '50%', background: '#fff', color: '#000', border: 'none', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0, transition: 'opacity .12s' },
    disclaimer:{ fontSize: 12, color: '#666', marginTop: 6, textAlign: 'center' },

    /* side panel (slide-over) */
    panelBackdrop: { position: 'absolute', inset: 0, zIndex: 20, background: 'rgba(0,0,0,.45)', display: 'flex', justifyContent: 'flex-end' },
    panelSheet:    { width: 520, maxWidth: '90%', background: '#1a1a1a', borderLeft: '1px solid #2a2a2a', overflowY: 'auto', height: '100%' },
    panelInner:    { padding: 24 },
    panelHead:     { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 },
    panelTitle:    { fontSize: 18, fontWeight: 600, margin: 0 },
    panelRefresh:  { background: '#2a2a2a', border: 'none', color: '#ccc', borderRadius: 8, padding: '6px 14px', cursor: 'pointer', fontSize: 12 },
    panelEmpty:    { color: '#666', textAlign: 'center', marginTop: 40 },

    /* job card */
    jobCard:   { background: '#212121', borderRadius: 10, padding: 14, marginBottom: 10, border: '1px solid #2a2a2a' },
    jobTop:    { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 },
    statusPill:{ fontSize: 10, fontWeight: 600, padding: '2px 10px', borderRadius: 10, color: '#fff', textTransform: 'uppercase', letterSpacing: '.04em' },
    track:     { height: 6, borderRadius: 3, background: '#333', overflow: 'hidden', marginBottom: 6 },
    bar:       { height: '100%', borderRadius: 3, transition: 'width .4s' },
    jobMeta:   { display: 'flex', justifyContent: 'space-between', fontSize: 11, color: '#888' },
    jobErr:    { fontSize: 12, color: '#f87171', marginTop: 6, padding: '6px 10px', background: '#2a1a1a', borderRadius: 6 },
    retryBtn:  { display: 'inline-flex', alignItems: 'center', gap: 6, marginTop: 8, padding: '5px 14px', background: '#333', border: 'none', borderRadius: 8, color: '#ccc', cursor: 'pointer', fontSize: 12 },
    pulse:     (c) => ({ display: 'inline-block', width: 8, height: 8, borderRadius: '50%', background: c, boxShadow: `0 0 6px ${c}`, animation: 'pulse 1.4s infinite' }),

    /* benchmark */
    benchBtn:  { flex: 1, padding: '10px 0', borderRadius: 10, border: 'none', background: '#a78bfa', color: '#fff', fontWeight: 600, fontSize: 13, cursor: 'pointer' },
    metricGrid:{ display: 'grid', gridTemplateColumns: '1fr auto', gap: '4px 12px', fontSize: 13 },
    metricLabel:{ color: '#888' },
    metricValue:{ fontWeight: 600, textAlign: 'right' },
    table:     { width: '100%', borderCollapse: 'collapse', fontSize: 12, marginTop: 8 },
    th:        { textAlign: 'left', padding: '8px 10px', borderBottom: '1px solid #333', color: '#888', fontWeight: 500 },
    thNum:     { textAlign: 'right', padding: '8px 10px', borderBottom: '1px solid #333', color: '#888', fontWeight: 500, whiteSpace: 'nowrap' },
    tdQuery:   { padding: '7px 10px', maxWidth: 260, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', color: '#ccc' },
    tdNum:     { padding: '7px 10px', textAlign: 'right', fontVariantNumeric: 'tabular-nums' },
};
