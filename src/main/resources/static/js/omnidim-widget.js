// OmniDimension voice widget loader.
// This is an alternative voice input path alongside the existing
// browser-based webkitSpeechRecognition flow in app.js. It embeds
// OmniDimension's hosted voice agent, which handles speech-to-text,
// multi-language NLU (including Hindi), and text-to-speech itself,
// then calls back into /api/voice/omnidim-webhook for actions.

(function () {
  const widgetId = document.body.dataset.omnidimWidgetId;
  if (!widgetId) {
    console.warn('OmniDimension widget ID not configured; skipping widget load.');
    return;
  }

  const script = document.createElement('script');
  script.src = 'https://widget.omnidim.io/widget.js';
  script.async = true;
  script.setAttribute('data-widget-id', widgetId);
  document.body.appendChild(script);
})();
