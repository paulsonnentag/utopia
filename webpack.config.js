const path = require('path');

module.exports = {
    watch: true,
    mode: 'development',
    entry: path.join(__dirname, 'src/main/libs/codemirror.js'),
    output: {
        path: path.join(__dirname, 'src/main/libs'),
        filename: 'bundle.js'
    },
    module: {
        rules: [
            {
                test: /\.js$/,
                exclude: /node_modules\/(?!(@codemirror\/next)\/).*/,
                use: 'babel-loader'
            }
        ]
    }
};
