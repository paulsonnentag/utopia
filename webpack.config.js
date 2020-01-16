const path = require('path');

module.exports = {
    watch: true,
    mode: 'development',
    entry: path.join(__dirname, 'src/main/libs/codemirror.jsx'),
    output: {
        path: path.join(__dirname, 'src/main/libs'),
        filename: 'bundle.js'
    },
    module: {
        rules: [
            {
                test: /\.jsx?$/,
                exclude: /node_modules\/(?!(@codemirror\/next)\/).*/,
                use: 'babel-loader'
            }
        ]
    }
};
