package com.example.woshield;


import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

public class SecuritySuggestionsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security_suggestions);

        ListView listView = findViewById(R.id.list_security_suggestions);
        String[] suggestions = {
                " Safety Guidelines for Women",



        "1. General Safety Tips",

        "Stay Aware: Always be aware of your surroundings, especially in unfamiliar places.",

                "Trust Your Instincts: If a place or person makes you feel uncomfortable, leave immediately.",

                "Keep Emergency Contacts Handy: Save important numbers on speed dial, such as family, friends, and local police.",

        "Avoid Sharing Personal Details: Be cautious when sharing personal information with strangers, both online and offline.",


        "2. Safety in Public Places",

        "Avoid Isolated Areas: Stay in well-lit, populated places, especially at night.",

        "Plan Your Route: Before heading out, plan your journey and share your location with a trusted contact.",

        "Use Reliable Transportation: Prefer official taxis, ride-sharing apps, or public transport with security measures.",

        "Carry Self-Defense Items: Items like pepper spray, a whistle, or a personal safety alarm can be helpful.",


        "3. Safety While Traveling Alone",

       " Share Your Itinerary: Inform a friend or family member about your travel plans and stay connected.",

                "Avoid Displaying Valuables: Keep jewelry, cash, and expensive gadgets out of sight to avoid attracting attention.",

                "Lock Doors and Windows: If staying in a hotel, always lock doors and check security arrangements.",


        "4. Safety at Work",

        "Know Your Workplace Security Measures: Be aware of exits, security personnel, and emergency protocols.",

        "Avoid Working Late Alone: If you must, inform someone about your whereabouts and use a safe mode of transport.",

                "Report Harassment: Do not hesitate to report any inappropriate behavior to HR or authorities.",


        "5. Safety on Social Media & Online",

        "Be Selective About What You Share: Avoid posting real-time location updates.",

        "Use Strong Passwords: Keep social media and bank accounts secure with strong passwords and two-factor authentication.",
                "Beware of Scammers: Do not share personal or financial details with unknown people online.",


        "6. Safety at Home",

        "Secure Doors and Windows: Always lock doors, especially at night or when alone.",

               " Do Not Open the Door to Strangers: Verify a visitor’s identity before allowing them inside.",

        "Keep Emergency Numbers Nearby: Have local police, family, and a trusted neighbor’s contact details saved.",


        "7. Self-Defense and Emergency Preparedness",

        "Take Self-Defense Training: Learning basic self-defense techniques can be empowering.",

        "Use Emergency Apps: Install women’s safety apps like bSafe, My Safetipin, or Raksha for instant help.",

        "Shout for Help: If in danger, call for help loudly to attract attention."


                };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                suggestions
        );
        listView.setAdapter(adapter);
    }
}
