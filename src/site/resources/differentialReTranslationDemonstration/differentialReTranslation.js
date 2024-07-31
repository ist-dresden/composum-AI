/* AIGenVersion(1516c86e, 3js.prompt-29a550a7, README.md-2deb7062, dialogelements.txt-4684af8d, requests.jsonl-5dddc8c5) */

// Function to handle translation request
async function handleTranslate() {
  const originalText = document.getElementById('originalTextField').value;
  const instructions = document.getElementById('instructionsField').value;
  const apiKey = localStorage.getItem('openai_api_key') || prompt('Enter OpenAI API Key:');
  if(apiKey) {
    localStorage.setItem('openai_api_key', apiKey);
  }

  const requestBody = {
    model: "gpt-4o-mini",
    messages: [
      {
        role: "system",
        content: "You are tasked as an expert translator to translate texts with utmost fidelity, preserving the original style, tone, sentiment, and all formatting elements (markdown, HTML tags, special characters) to the greatest extent possible.\nIMPORTANT: Only provide the translated text, maintaining all original formatting and non-translatable elements. Avoid any extraneous comments or actions not directly related to the translation."
      },
      {
        role: "user",
        content: "Print the original text you have to translate exactly without any comments."
      },
      {
        role: "assistant",
        content: originalText
      },
      {
        role: "user",
        content: instructions
      }
    ]
  };

  try {
    document.getElementById('translateButton').disabled = true;
    console.log('Sending translation request:', requestBody);
    const response = await fetch('https://api.openai.com/v1/chat/completions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + apiKey
      },
      body: JSON.stringify(requestBody)
    });

    if (response.status === 200) {
      const data = await response.json();
      console.log('Received response:', data);
      document.getElementById('autoTranslatedTextField').value = data.choices[0].message.content;
    } else {
      alert('Failed to translate text. Response status: ' + response.status);
    }
  } catch (error) {
    console.error('Error during translation request:', error);
    alert('An error occurred during the translation request.');
  } finally {
    document.getElementById('translateButton').disabled = false;
  }
}

// Function to handle differential re-translation request
async function handleRetranslate() {
  const originalText = document.getElementById('originalTextField').value;
  const instructions = document.getElementById('instructionsField').value;
  const autoTranslatedText = document.getElementById('autoTranslatedTextField').value;
  const manuallyCorrectedTranslation = document.getElementById('correctedTextField').value;
  const revisedOriginalText = document.getElementById('changedTextField').value;
  const apiKey = localStorage.getItem('openai_api_key');

  const requestBody = {
    model: "gpt-4o-mini",
    messages: [
      {
        role: "system",
        content: "You are tasked as an expert translator to translate texts with utmost fidelity, preserving the original style, tone, sentiment, and all formatting elements (markdown, HTML tags, special characters) to the greatest extent possible.\nIMPORTANT: Only provide the translated text, maintaining all original formatting and non-translatable elements. Avoid any extraneous comments or actions not directly related to the translation."
      },
      {
        role: "user",
        content: "Print the original text you have to translate exactly without any comments."
      },
      {
        role: "assistant",
        content: originalText
      },
      {
        role: "user",
        content: instructions
      },
      {
        role: "assistant",
        content: autoTranslatedText
      },
      {
        role: "user",
        content: "Print this original text as it was manually adapted."
      },
      {
        role: "assistant",
        content: manuallyCorrectedTranslation
      },
      {
        role: "user",
        content: "Print the new text that is to be translated."
      },
      {
        role: "assistant",
        content: revisedOriginalText
      },
      {
        role: "user",
        content: "Translate the new text. Take care to include the manual adaptions for the original text."
      }
    ]
  };

  try {
    document.getElementById('retranslateButton').disabled = true;
    console.log('Sending differential re-translation request:', requestBody);
    const response = await fetch('https://api.openai.com/v1/chat/completions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + apiKey
      },
      body: JSON.stringify(requestBody)
    });

    if (response.status === 200) {
      const data = await response.json();
      console.log('Received response:', data);
      document.getElementById('retranslatedResultField').value = data.choices[0].message.content;
    } else {
      alert('Failed to re-translate text. Response status: ' + response.status);
    }
  } catch (error) {
    console.error('Error during differential re-translation request:', error);
    alert('An error occurred during the differential re-translation request.');
  } finally {
    document.getElementById('retranslateButton').disabled = false;
  }
}

// Bind functions to buttons
document.getElementById('translateButton').addEventListener('click', handleTranslate);
document.getElementById('retranslateButton').addEventListener('click', handleRetranslate);
