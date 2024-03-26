/* AIGenVersion(0cae8e86, 4examplejs.prompt-57d98546, README.md-0f3151bb, dialogelements.txt-4684af8d, examples.txt-714c4162) */

// Define the examples data
const examples = [
    {
        name: "Our Contributors",
        originalText: "Our Contributors",
        instructions: "Translate this text into German",
        autoTranslatedText: "Unsere Mitwirkenden",
        manuallyCorrectedText: "Unsere Autoren",
        changedOriginalText: "Meet our contributors",
        retranslatedResult: "Treffe unsere Autoren"
    },
    {
        name: "Travel Guides",
        originalText: "Meet our extraordinary travel guides. When you travel with a certified WKND guide you gain access to attractions and perspectives not found on the pages of a guide book.",
        instructions: "Translate this text into German",
        autoTranslatedText: "Lernen Sie unsere außergewöhnlichen Reiseführer kennen. Wenn Sie mit einem zertifizierten WKND-Reiseleiter unterwegs sind, erhalten Sie Zugang zu Attraktionen und Perspektiven, die nicht auf den Seiten eines Reiseführers zu finden sind.",
        manuallyCorrectedText: "Lernen Sie unsere außergewöhnlichen Reiseleiter kennen. Wenn Sie mit einem zertifizierten WKND-Reiseleiter unterwegs sind, erhalten Sie Zugang zu Attraktionen und Perspektiven, die nicht auf den Seiten eines Reiseführers zu finden sind.",
        changedOriginalText: "Meet our extraordinary travel guides. When you travel with a certified WKND guide you gain access to attractions and perspectives not found on the pages of a guide book.",
        retranslatedResult: "Lernen Sie unsere außergewöhnlichen Reiseleiter kennen. Wenn Sie mit einem zertifizierten WKND-Reiseleiter unterwegs sind, erhalten Sie Zugang zu Attraktionen und Perspektiven, die nicht auf den Seiten eines Reiseführers zu finden sind. Wählen Sie den perfekten Reiseleiter für sich!"
    },
    {
        name: "Sustainable living",
        originalText: "Discover the latest trends in sustainable living.",
        instructions: "Translate to German, aiming for a young, eco-conscious audience. Use informal 'du' and include relevant cultural references to Germany.",
        autoTranslatedText: "Entdecke die neuesten Trends im nachhaltigen Leben.",
        manuallyCorrectedText: "Entdecke die neuesten Trends für ein umweltbewusstes Leben.",
        changedOriginalText: "Explore the latest trends in sustainable living in urban environments.",
        retranslatedResult: "Entdecke die neuesten Trends für ein umweltbewusstes Leben in städtischen Umgebungen."
    }
];

// Populate the dropdown list with examples
const dropdown = document.getElementById('examplesDropdown');

// Add a default option
const defaultOption = document.createElement('option');
defaultOption.text = "Select an example";
dropdown.add(defaultOption);

// Add each example to the dropdown
examples.forEach((example, index) => {
    const option = document.createElement('option');
    option.text = example.name;
    option.value = index; // Use the index as the value for easy retrieval of example data
    dropdown.add(option);
});

// Function to fill the form fields with the selected example data
function fillFormWithExampleData(index) {
    const example = examples[index];
    document.getElementById('originalTextField').value = example.originalText;
    document.getElementById('instructionsField').value = example.instructions;
    document.getElementById('autoTranslatedTextField').value = example.autoTranslatedText;
    document.getElementById('correctedTextField').value = example.manuallyCorrectedText;
    document.getElementById('changedTextField').value = example.changedOriginalText;
    document.getElementById('retranslatedResultField').value = example.retranslatedResult;
}

// Event listener for dropdown change
dropdown.addEventListener('change', function() {
    const selectedIndex = dropdown.value;
    if (selectedIndex !== "Select an example") {
        fillFormWithExampleData(selectedIndex);
    }
});
