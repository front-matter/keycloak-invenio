<html>
<head>
  <style>
    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
    .container { max-width: 600px; margin: 20px auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px; }
    .code { font-size: 32px; font-weight: bold; color: #0066cc; letter-spacing: 3px; margin: 20px 0; text-align: center; }
    .info { background-color: #f0f0f0; padding: 15px; border-radius: 3px; margin: 15px 0; }
    .button { display: inline-block; padding: 12px 24px; background-color: #0066cc; color: #ffffff; text-decoration: none; border-radius: 4px; font-weight: bold; margin: 15px 0; }
    .button:hover { background-color: #0052a3; }
  </style>
</head>
<body>
  <div class="container">
    <h2>Your Verification Code</h2>
    <div class="code" id="code-value">${code}</div>
    <div class="info">
      <p><strong>Important:</strong> This code will expire in ${ttl} seconds.</p>
      <p>Click the button below to automatically log in with this code:</p>
      <div style="text-align: center;">
        <a href="${loginActionUrl!''}?code=${code}" class="button">Log In Automatically</a>
      </div>
      <p style="font-size: 0.9em; color: #666; margin-top: 15px;">
        Or copy the code above and paste it manually: <a href="${realmUrl!''}/account">Go to Login Page</a>
      </p>
    </div>
    <p>If you did not request this code, please ignore this email.</p>
  </div>
</body>
</html>
