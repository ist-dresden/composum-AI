/** A class that opens a dialog with a help text retrieved via a given URL in the event target. */
class HelpPage {
    constructor(event) {
        console.log('HelpPage constructor', event);
        event.preventDefault();
        event.stopPropagation();
        this.url = $(event.target).data('href');
        this.id = 'composum-ai-help-dialog-' + this.url.replace(/[^a-zA-Z0-9]+/g, '-');
        this.dialog = document.getElementById(this.id);
    }

    show() {
        console.log('showing help page', this.url);
        if (!this.dialog) {
            this.dialog = new Coral.Dialog().set({
                id: this.id,
                variant: 'HELP',
                header: {
                    innerHTML: Granite.I18n.get('Help')
                },
                content: {
                    innerHTML: '<div class="helpcontent"></div>'
                },
                footer: {
                    innerHTML: '<button is="coral-button" variant="primary" coral-close size="M">' + Granite.I18n.get('Ok') + '</button>'
                }
            });
            document.body.appendChild(this.dialog);
            $.ajax(this.url).done((data) => {
                console.log('help page data', data);
                this.dialog.querySelector('.helpcontent').innerHTML = data;
                this.dialog.show();
            }).fail((error) => console.log('error loading help page', error));
        } else {
            this.dialog.show();
        }
    }

}

export {HelpPage};
