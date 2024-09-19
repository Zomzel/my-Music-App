const express = require('express');
const mysql = require('mysql2');
const bcrypt = require('bcryptjs');
const bodyParser = require('body-parser');
const cors = require('cors');


const app = express();
app.use(bodyParser.json());
app.use(cors()); // 允许跨域请求

// 连接 MySQL 数据库
const db = mysql.createConnection({
    host: 'localhost',
    user: 'root',  // 请根据你的配置调整用户名和密码
    password: 'zomzel1020',
    database: 'myMusic'
});

db.connect(err => {
    if (err) throw err;
    console.log('MySQL Connected...');
});

// 注册用户
app.post('/register', (req, res) => {
    const { username, password } = req.body;

    if (!username || !password) {
        return res.status(400).json({ message: '用户名或密码不能为空' });
    }

    const hashedPassword = bcrypt.hashSync(password, 8);

    // 插入用户数据到数据库
    const query = 'INSERT INTO users (username, password) VALUES (?, ?)';
    db.query(query, [username, hashedPassword], (err, result) => {
        if (err) {
            if (err.code === 'ER_DUP_ENTRY') {
                return res.status(400).json({ message: '用户名已存在' });
            }
            throw err;
        }
        res.status(201).json({ message: '注册成功' });
    });
});

// 用户登录
app.post('/login', (req, res) => {
    const { username, password } = req.body;

    const query = 'SELECT * FROM users WHERE username = ?';
    db.query(query, [username], (err, results) => {
        if (err) throw err;

        if (results.length === 0) {
            return res.status(400).json({ message: '用户名或密码错误' });
        }

        const user = results[0];
        const isPasswordValid = bcrypt.compareSync(password, user.password);

        if (!isPasswordValid) {
            return res.status(400).json({ message: '用户名或密码错误' });
        }

        res.status(200).json({ message: '登录成功' });
    });
});

// 启动服务器
app.listen(3000,'0.0.0.0', () => {
    console.log('Server started on http://localhost:3000');
});
