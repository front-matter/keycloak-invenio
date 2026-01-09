<html>
<head>
  <style>
    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
    .container { max-width: 600px; margin: 20px auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px; }
    .code { font-size: 32px; font-weight: bold; color: #0066cc; letter-spacing: 3px; margin: 20px 0; text-align: center; }
    .info { background-color: #f0f0f0; padding: 15px; border-radius: 3px; margin: 15px 0; }
  </style>
</head>
<body>
  <div class="container">
    <h2>Your Verification Code</h2>
    <div class="code">${code}</div>
    <div class="info">
      <p><strong>Important:</strong> This code will expire in ${ttl} seconds.</p>
      <p>Please return to your browser window to enter this code and complete your login.</p>
    </div>
    <p>If you did not request this code, please ignore this email.</p>
  </div>
</body>
</html>
