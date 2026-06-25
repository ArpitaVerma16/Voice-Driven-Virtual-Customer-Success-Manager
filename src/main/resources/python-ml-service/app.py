from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from datetime import datetime, timedelta
import pandas as pd
import numpy as np
from prophet import Prophet
from sklearn.ensemble import RandomForestRegressor
from sklearn.preprocessing import StandardScaler
import joblib
import json
import os
from typing import List, Optional

app = FastAPI(title="VCSM ML Service", version="1.0.0")

# Enable CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Models
class ComplaintPredictionRequest(BaseModel):
    days: int = 7

class EventPredictionRequest(BaseModel):
    eventId: int
    historicalData: List[dict]

class SentimentPredictionRequest(BaseModel):
    historicalSentiment: List[dict]

# Response Models
class PredictionResponse(BaseModel):
    predictions: List[dict]
    confidence: float
    recommendation: str

class DailyPrediction(BaseModel):
    date: str
    predicted_count: int
    lower_bound: int
    upper_bound: int

# ============================================================
# 1. COMPLAINT VOLUME PREDICTION (Prophet)
# ============================================================

def generate_sample_data(days=30):
    """Generate sample historical data for demonstration"""
    dates = pd.date_range(start=datetime.now() - timedelta(days=days), periods=days)
    # Base pattern: more complaints on weekends
    data = []
    for i, date in enumerate(dates):
        # Weekend effect
        weekend_effect = 0.5 if date.weekday() >= 5 else 0
        # Random variation
        random_var = np.random.normal(0, 3)
        # Base count with trend
        base = 5 + i * 0.1 + weekend_effect * 8
        count = max(0, int(base + random_var))
        data.append({'ds': date.strftime('%Y-%m-%d'), 'y': count})
    return pd.DataFrame(data)

@app.post("/api/predict/complaints", response_model=PredictionResponse)
async def predict_complaints(request: ComplaintPredictionRequest):
    """
    Predict complaint volume for next N days using Prophet
    """
    try:
        # Generate historical data (in production, fetch from DB)
        df = generate_sample_data(30)
        
        # Train Prophet model
        model = Prophet(
            yearly_seasonality=False,
            weekly_seasonality=True,
            daily_seasonality=False,
            changepoint_prior_scale=0.05
        )
        model.fit(df)
        
        # Make future predictions
        future = model.make_future_dataframe(periods=request.days)
        forecast = model.predict(future)
        
        # Extract predictions
        predictions = []
        for i in range(len(forecast) - len(df)):
            row = forecast.iloc[-(i+1)]
            predictions.insert(0, {
                'date': row['ds'].strftime('%Y-%m-%d'),
                'predicted_count': int(round(max(0, row['yhat']))),
                'lower_bound': int(round(max(0, row['yhat_lower']))),
                'upper_bound': int(round(row['yhat_upper']))
            })
        
        # Calculate confidence (based on prediction intervals)
        avg_interval = np.mean([abs(p['upper_bound'] - p['lower_bound']) / 2 for p in predictions])
        confidence = max(0, min(100, 95 - avg_interval * 0.5))
        
        # Generate recommendation
        avg_pred = np.mean([p['predicted_count'] for p in predictions])
        if avg_pred > 15:
            recommendation = "⚠️ High complaint volume expected. Consider increasing support staff."
        elif avg_pred > 8:
            recommendation = "📊 Moderate complaint volume expected. Regular staffing recommended."
        else:
            recommendation = "✅ Low complaint volume expected. Maintain normal operations."
        
        return PredictionResponse(
            predictions=predictions,
            confidence=round(confidence, 1),
            recommendation=recommendation
        )
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ============================================================
# 2. EVENT ATTENDANCE PREDICTION
# ============================================================

@app.post("/api/predict/event/{event_id}")
async def predict_event_attendance(event_id: int, request: EventPredictionRequest):
    """
    Predict event attendance based on historical data
    """
    try:
        # Extract features from historical data
        if not request.historicalData:
            # Generate sample data if none provided
            sample_data = []
            for i in range(10):
                sample_data.append({
                    'registrations': np.random.randint(10, 50),
                    'views': np.random.randint(50, 200),
                    'days_before': np.random.randint(1, 7),
                    'capacity': 100
                })
            request.historicalData = sample_data
        
        df = pd.DataFrame(request.historicalData)
        
        # Prepare features
        features = ['registrations', 'views', 'days_before', 'capacity']
        X = df[features].values
        y = df['registrations'].values
        
        # Simple prediction (weighted average)
        if len(df) > 1:
            weights = np.linspace(1, 2, len(df))
            avg_registrations = np.average(y, weights=weights[-len(y):])
        else:
            avg_registrations = y[0] if len(y) > 0 else 25
        
        # Add some variation based on views
        avg_views = np.mean(df['views']) if 'views' in df else 100
        attendance_boost = (avg_views / 100) * 5
        
        predicted_attendance = min(
            int(round(avg_registrations + attendance_boost)),
            100  # capacity limit
        )
        
        confidence = min(95, 60 + (len(df) * 3))
        confidence = min(confidence, 95)
        
        return {
            'eventId': event_id,
            'predicted_attendance': predicted_attendance,
            'confidence': round(confidence, 1),
            'recommendation': 'The event is expected to have good attendance. Consider preparing for ' + str(predicted_attendance) + ' participants.'
        }
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ============================================================
# 3. SENTIMENT TREND PREDICTION
# ============================================================

@app.post("/api/predict/sentiment")
async def predict_sentiment(request: SentimentPredictionRequest):
    """
    Predict future sentiment trends based on historical data
    """
    try:
        # Use historical sentiment data
        if not request.historicalSentiment:
            # Generate sample data
            sentiments = []
            for i in range(7):
                sentiments.append({
                    'date': (datetime.now() - timedelta(days=i)).strftime('%Y-%m-%d'),
                    'sentiment_score': np.random.uniform(-0.5, 0.8)
                })
            request.historicalSentiment = sentiments
        
        df = pd.DataFrame(request.historicalSentiment)
        df['date'] = pd.to_datetime(df['date'])
        df = df.sort_values('date')
        
        # Calculate trend (linear regression)
        if len(df) > 1:
            x = np.arange(len(df))
            y = df['sentiment_score'].values
            
            # Simple linear trend
            slope = np.polyfit(x, y, 1)[0]
            
            # Predict next 3 days
            last_score = y[-1]
            predictions = []
            current_date = df['date'].iloc[-1]
            
            for i in range(1, 4):
                next_day = current_date + timedelta(days=i)
                pred_score = last_score + slope * i
                pred_score = max(-1, min(1, pred_score))
                predictions.append({
                    'date': next_day.strftime('%Y-%m-%d'),
                    'sentiment_score': round(pred_score, 3),
                    'sentiment_label': get_sentiment_label(pred_score)
                })
            
            # Overall trend
            if slope > 0.05:
                trend = "📈 Improving sentiment"
                recommendation = "Sentiment is improving. Continue positive engagement."
            elif slope < -0.05:
                trend = "📉 Declining sentiment"
                recommendation = "⚠️ Sentiment is declining. Consider proactive outreach."
            else:
                trend = "➡️ Stable sentiment"
                recommendation = "Sentiment is stable. Maintain current approach."
            
            return {
                'predictions': predictions,
                'trend': trend,
                'recommendation': recommendation,
                'confidence': round(80 - abs(slope * 20), 1)
            }
        else:
            return {
                'predictions': [],
                'trend': "Insufficient data",
                'recommendation': "Need more sentiment data for accurate prediction",
                'confidence': 0
            }
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ============================================================
# 4. PEAK TIME IDENTIFICATION
# ============================================================

@app.get("/api/predict/peak-times")
async def get_peak_times():
    """
    Identify peak complaint times based on historical patterns
    """
    try:
        # Generate sample hourly data
        hours = list(range(24))
        # Peak at 10am and 2pm
        peak_pattern = [2, 2, 3, 4, 6, 8, 10, 12, 15, 18, 20, 18, 15, 12, 10, 8, 6, 4, 3, 2, 2, 1, 1, 1]
        
        # Add some randomness
        np.random.seed(42)
        values = [int(p + np.random.normal(0, 1)) for p in peak_pattern]
        values = [max(0, v) for v in values]
        
        peak_time = max(range(len(values)), key=lambda i: values[i])
        
        recommendations = {
            'peak_hours': [10, 14, 16],
            'peak_time': f"{peak_time:02d}:00",
            'peak_activity_level': max(values),
            'recommendation': f"Peak complaint time is {peak_time:02d}:00. Consider extra staff during this time.",
            'hourly_distribution': {f"{h:02d}:00": v for h, v in enumerate(values)}
        }
        
        return recommendations
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ============================================================
# Helper Functions
# ============================================================

def get_sentiment_label(score):
    if score > 0.5:
        return "Very Positive"
    elif score > 0.1:
        return "Positive"
    elif score > -0.1:
        return "Neutral"
    elif score > -0.5:
        return "Negative"
    else:
        return "Very Negative"

# Health Check
@app.get("/health")
async def health_check():
    return {"status": "healthy", "service": "VCSM ML Service"}

# ============================================================
# Run with: uvicorn app:app --host 0.0.0.0 --port 8000 --reload
# ============================================================
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)