import React from 'react'
import { EditorView, ViewPlugin } from '@codemirror/next/view';
import { EditorState } from '@codemirror/next/state';
import { keymap } from '@codemirror/next/keymap';
import { history, redo, undo } from '@codemirror/next/history';
import { baseKeymap, indentSelection } from '@codemirror/next/commands';
import { bracketMatching } from '@codemirror/next/matchbrackets';
import { closeBrackets } from '@codemirror/next/closebrackets';

const isMac = /Mac/.test(navigator.platform);

class CodeEditor extends React.Component {

    constructor(props) {
        super(props);

        this.containerRef = React.createRef();
    }

    componentDidMount() {
        const {value, onChange, onSave} = this.props;

        this.editor = new EditorView({
            state: EditorState.create({
                doc: value,
                extensions: [
                    history(),
                    closeBrackets,
                    bracketMatching(),
                    changeListener(onChange),
                    keymap({
                        'Mod-s': () => {
                            onSave()
                            return true;
                        },
                        'Mod-z': undo,
                        'Mod-Shift-z': redo,
                        'Ctrl-y': isMac ? undefined : redo,
                        'Shift-Tab': indentSelection,
                    }),
                    keymap(baseKeymap)
                ]
            }),

            handleDOMEvents: {
                'keydown': (evt) => {
                    console.log('keydown', evt)
                }
            }
        });

        this.containerRef.current.appendChild(this.editor.dom);
    }

    componentWillUnmount() {
        this.editor.destroy();
    }

    render () {
        return (
            <div ref={this.containerRef}/>
        )
    }

    disconnectedCallback() {
        this.editor.destroy();
    }
}

window.codeMirror = {
    CodeEditor
}


class ChangeListenerPlugin {
    constructor(callback) {
        this.callback = callback
    }

    update(update) {
        if (update.docChanged) {
            this.callback(update.state.doc.toString());
        }
    }
}

function changeListener(callback) {
    const plugin = (
        ViewPlugin
            .create(view => new ChangeListenerPlugin(callback))
    );

    return [
        plugin.extension
    ];
}
