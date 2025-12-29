# WoShield - Women Safety Android Application

WoShield is a comprehensive safety application designed specifically for women's security. The app provides multiple safety features including emergency contacts, fake calls, location sharing, and AI-powered chatbot support.

## Features

- **Emergency Contact System**: Quickly reach out to trusted contacts in case of emergencies
- **Fake Call Feature**: Generate fake calls to help escape uncomfortable situations
- **Location Tracking**: Share your real-time location with trusted contacts
- **Safety Zone Detection**: Identify safe zones nearby
- **AI Chatbot**: Access to an AI-powered chatbot for support and guidance
- **Voice Help**: Voice-activated assistance for hands-free operation
- **Security Suggestions**: Get personalized security recommendations
- **Geofencing**: Receive alerts when entering or leaving specific areas

## Technologies Used

- **Platform**: Android (Java)
- **Maps**: Google Maps API for location services
- **AI Integration**: Hugging Face API for chatbot functionality
- **Authentication**: Firebase Authentication
- **Database**: Firebase Realtime Database
- **Push Notifications**: Firebase Cloud Messaging
- **HTTP Client**: OkHttp for API requests
- **Image Loading**: Glide for image handling

## Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/DhangarRohit18/woshield.git
   ```

2. Open the project in Android Studio

3. Add your API keys to `gradle.properties`:
   ```properties
   HUGGING_FACE_API_KEY=your_actual_api_key_here
   ```

4. Build and run the application

## Configuration

### API Keys
To use the AI chatbot feature, you need to provide your own Hugging Face API key:

1. Get your Hugging Face API key from [Hugging Face](https://huggingface.co/settings/tokens)
2. Add it to your `gradle.properties` file:
   ```
   HUGGING_FACE_API_KEY=your_actual_api_key_here
   ```

### Firebase Setup
The app uses Firebase for authentication and database services. To fully use the app:

1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Add your Android app to the project
3. Download the `google-services.json` file and place it in the `app/` directory
4. Enable the required Firebase services (Authentication, Realtime Database, etc.)

## Usage

1. **Sign Up/Sign In**: Create an account or sign in to access the app features
2. **Set Emergency Contacts**: Add trusted contacts who can be notified in emergencies
3. **Use Safety Features**: Access various safety features from the home screen
4. **Chat with AI**: Use the chatbot for safety advice and support

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## Security Considerations

- API keys are handled securely using BuildConfig fields
- Sensitive information is not stored in the repository
- Follow Android security best practices

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Google Maps API for location services
- Hugging Face for AI model access
- Firebase for backend services
- Android Open Source Project