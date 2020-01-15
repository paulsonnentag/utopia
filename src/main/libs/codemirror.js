import { EditorView } from '@codemirror/next/view/dist/index.js';
import { EditorState } from '@codemirror/next/state/dist/index.js';

class CodeEditor extends HTMLElement {

    static get observedAttributes() {
        return ['value'];
    }

    connectedCallback() {
        const value = this.attributes.value ? this.attributes.value.value : '';

        this.editor = new EditorView({
            state: EditorState.create({ doc: value })
        });

        this.appendChild(this.editor.dom);
    }

    disconnectedCallback() {
        this.editor.destroy();
    }

    attributeChangedCallback(name, oldValue, newValue) {
        switch (name) {
            case 'value':
                console.log('changed', newValue);

        }
    }
}

if (customElements.get('code-editor')) {
    location.reload();
} else {
    customElements.define('code-editor', CodeEditor);
}


