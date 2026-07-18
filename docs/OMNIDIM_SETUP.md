# OmniDimension Voice AI Setup

This document covers the manual dashboard steps needed to complete the
Omnidim.io integration. These steps cannot be automated in code and must
be done once by a maintainer with an OmniDimension account.

## 1. Create an account and agent
1. Sign up at https://omnidim.io
2. From the dashboard, create a new Agent (e.g. "VCSM Voice Assistant")
3. Enable the languages you need under agent settings - Hindi and other
   regional languages are supported out of the box (90+ languages)

## 2. Connect the Custom API Integration
1. In the agent's configuration, go to Integrations then Custom API
2. Add a new integration:
   - URL: https://your-deployed-host/api/voice/omnidim-webhook
   - Method: POST
   - Body params: transcript (string, AI-generated from conversation),
     language (string, e.g. en, hi)
3. Enable "AI Generated" for the transcript field so the agent fills it
   in automatically from what the user says
4. Save and attach this integration to the agent

## 3. Get your Widget ID
1. Go to Deploy then Web Chat Widget
2. Copy the generated Widget ID

## 4. Configure the app
1. Set the environment variable OMNIDIM_WIDGET_ID to the widget ID from
   step 3, and OMNIDIM_API_KEY to your API key
2. Add data-omnidim-widget-id="YOUR_WIDGET_ID" to the body tag in
   the relevant template (e.g. dashboard.html)
3. Include omnidim-widget.js on that page

## Notes
- The existing browser webkitSpeechRecognition flow in app.js is left
  in place as a fallback for browsers/users without the widget enabled
- OmnidimService.processVoiceCommand() is reused as-is for intent
  handling - the webhook just gives OmniDimension's agent a way to call it
