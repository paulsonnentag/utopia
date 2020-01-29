import * as React from 'react'
import {Decoration, EditorView, ViewPlugin} from '@codemirror/next/view';
import {EditorState} from '@codemirror/next/state';
import {keymap} from '@codemirror/next/keymap';
import {history, redo, undo} from '@codemirror/next/history';
import {baseKeymap, indentSelection} from '@codemirror/next/commands';
import {bracketMatching} from '@codemirror/next/matchbrackets';
import {closeBrackets} from '@codemirror/next/closebrackets';
import {RefObject} from "react";

const isMac = /Mac/.test(navigator.platform);

interface CodeEditorProps {
    value: string
    onChange: (value: string) => void
    onSave: () => void
}

export class CodeEditor extends React.Component<CodeEditorProps, {}> {

    containerRef: RefObject<HTMLDivElement>;

    editor: EditorView;

    constructor(props: CodeEditorProps) {
        super(props);

        const {value, onChange, onSave} = props;

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
                            onSave();
                            return true;
                        },
                        'Mod-z': undo,
                        'Mod-Shift-z': redo,
                        'Ctrl-y': isMac ? undefined : redo,
                        'Shift-Tab': indentSelection,
                    }),
                    keymap(baseKeymap)
                ]
            })
        });

        this.containerRef = React.createRef<HTMLDivElement>();
    }

    componentDidMount() {
        if (this.containerRef.current) {
            this.containerRef.current.appendChild(this.editor.dom);
        }
    }

    componentWillUnmount() {
        this.editor.destroy();
    }

    render() {
        return (
            <div ref={this.containerRef}/>
        )
    }

    disconnectedCallback() {
        this.editor.destroy();
    }
}

// @ts-ignore
window.codemirror = {
    CodeEditor
};

function changeListener(callback: (value: string) => void) {
    return [
        ViewPlugin.decoration({
            create() {
                return Decoration.none
            },

            update(deco, update) {
                if (update.docChanged) {
                    callback(update.state.doc.toString());
                }

                return Decoration.none;
            }
        })
    ];
}
