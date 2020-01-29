const path = require('path');

module.exports = {
    watch: true,
    mode: 'development',
    entry: path.join(__dirname, 'libs/codemirror.tsx'),
    output: {
        path: path.join(__dirname, 'target'),
        filename: 'bundle.js'
    },
    module: {
        rules: [
            {
                test: /\.tsx?$/,
                use: 'ts-loader',
                exclude: /node_modules/,
            },
        ],
    },
    resolve: {
        extensions: [ '.tsx', '.ts', '.js' ],
    },
};
