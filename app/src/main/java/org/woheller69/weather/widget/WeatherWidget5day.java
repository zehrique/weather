package org.woheller69.weather.widget;


import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

import org.woheller69.weather.R;
import org.woheller69.weather.activities.ForecastCityActivity;
import org.woheller69.weather.database.CityToWatch;
import org.woheller69.weather.database.Forecast;
import org.woheller69.weather.database.PFASQLiteHelper;
import org.woheller69.weather.database.WeekForecast;
import org.woheller69.weather.services.UpdateDataService;
import org.woheller69.weather.ui.Help.StringFormatUtils;
import org.woheller69.weather.ui.UiResourceProvider;
import org.woheller69.weather.weather_api.IApiToDatabaseConversion;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import static androidx.core.app.JobIntentService.enqueueWork;
import static org.woheller69.weather.services.UpdateDataService.SKIP_UPDATE_INTERVAL;
import static org.woheller69.weather.ui.RecycleList.CityWeatherAdapter.checkSun;
import static org.woheller69.weather.ui.RecycleList.CityWeatherAdapter.getCorrectedWeatherID;

public class WeatherWidget5day extends AppWidgetProvider {

    public void updateAppWidget(Context context, final int appWidgetId) {

        PFASQLiteHelper db = PFASQLiteHelper.getInstance(context);
        if (!db.getAllCitiesToWatch().isEmpty()) {

            int cityID = getWidgetCityID(context);

            Intent intent = new Intent(context, UpdateDataService.class);
            //Log.d("debugtag", "widget calls single update: " + cityID + " with widgetID " + appWidgetId);

            intent.setAction(UpdateDataService.UPDATE_SINGLE_ACTION);
            intent.putExtra("cityId", cityID);
            intent.putExtra("Widget",true);
            intent.putExtra(SKIP_UPDATE_INTERVAL, true);
            enqueueWork(context, UpdateDataService.class, 0, intent);
        }
    }

    public static int getWidgetCityID(Context context) {
        PFASQLiteHelper db = PFASQLiteHelper.getInstance(context);
        int cityID=0;
        List<CityToWatch> cities = db.getAllCitiesToWatch();
        int rank=cities.get(0).getRank();
        for (int i = 0; i < cities.size(); i++) {   //find cityID for first city to watch = lowest Rank
            CityToWatch city = cities.get(i);
            //Log.d("debugtag",Integer.toString(city.getRank()));
            if (city.getRank() <= rank ){
                rank=city.getRank();
                cityID = city.getCityId();
            }
         }
        return cityID;
}

    public static void updateView(Context context, AppWidgetManager appWidgetManager, RemoteViews views, int appWidgetId, CityToWatch city, List<Forecast> forecasts, List<WeekForecast> weekforecasts) {

        int cityId=getWidgetCityID(context);
        PFASQLiteHelper database = PFASQLiteHelper.getInstance(context.getApplicationContext());
        int zonemilliseconds = database.getCurrentWeatherByCityId(cityId).getTimeZoneSeconds()*1000;

        Calendar c = Calendar.getInstance();
        c.setTimeZone(TimeZone.getTimeZone("GMT"));

        int []forecastData = new int[5];
        String []weekday = new String[5];
        for (int i=0;i<5;i++){
            c.setTimeInMillis(weekforecasts.get(i).getForecastTime()+zonemilliseconds);
            int day = c.get(Calendar.DAY_OF_WEEK);
            weekday[i]=context.getResources().getString(StringFormatUtils.getDayShort(day));

            forecastData[i]=weekforecasts.get(i).getWeatherID();

                if ((forecastData[i] >= IApiToDatabaseConversion.WeatherCategories.LIGHT_RAIN.getNumVal()) && (forecastData[i] <= IApiToDatabaseConversion.WeatherCategories.RAIN.getNumVal())) {
                    if (checkSun(context, cityId,weekforecasts.get(i).getForecastTime())) {
                        forecastData[i] = IApiToDatabaseConversion.WeatherCategories.SHOWER_RAIN.getNumVal(); //if at least one interval with sun +/-5 from noon, use shower rain instead of rain
                        if (getCorrectedWeatherID(context, cityId,weekforecasts.get(i).getForecastTime()) < forecastData[i])
                            forecastData[i] = getCorrectedWeatherID(context, cityId,weekforecasts.get(i).getForecastTime()); //if always sun use worst sun category
                    }
                }
                if ((forecastData[i] >= IApiToDatabaseConversion.WeatherCategories.LIGHT_SNOW.getNumVal()) && (forecastData[i] <= IApiToDatabaseConversion.WeatherCategories.HEAVY_SNOW.getNumVal())) {
                    if (checkSun(context, cityId,weekforecasts.get(i).getForecastTime())) {
                        forecastData[i] = IApiToDatabaseConversion.WeatherCategories.SHOWER_SNOW.getNumVal();
                        if (getCorrectedWeatherID(context, cityId,weekforecasts.get(i).getForecastTime()) < forecastData[i])
                            forecastData[i] = getCorrectedWeatherID(context, cityId,weekforecasts.get(i).getForecastTime());
                    }
                }
                if (forecastData[i] == IApiToDatabaseConversion.WeatherCategories.RAIN_SNOW.getNumVal()) {
                    if (checkSun(context, cityId,weekforecasts.get(i).getForecastTime())) {
                        forecastData[i] = IApiToDatabaseConversion.WeatherCategories.SHOWER_RAIN_SNOW.getNumVal();
                        if (getCorrectedWeatherID(context, cityId,weekforecasts.get(i).getForecastTime()) < forecastData[i])
                            forecastData[i] = getCorrectedWeatherID(context, cityId,weekforecasts.get(i).getForecastTime());
                    }
                }

        }

        views.setImageViewResource(R.id.widget_5day_image1, UiResourceProvider.getIconResourceForWeatherCategory(forecastData[0], true));
        views.setImageViewResource(R.id.widget_5day_image2, UiResourceProvider.getIconResourceForWeatherCategory(forecastData[1], true));
        views.setImageViewResource(R.id.widget_5day_image3, UiResourceProvider.getIconResourceForWeatherCategory(forecastData[2], true));
        views.setImageViewResource(R.id.widget_5day_image4, UiResourceProvider.getIconResourceForWeatherCategory(forecastData[3], true));
        views.setImageViewResource(R.id.widget_5day_image5, UiResourceProvider.getIconResourceForWeatherCategory(forecastData[4], true));

        views.setTextViewText(R.id.widget_5day_day1,weekday[0]);
        views.setTextViewText(R.id.widget_5day_day2,weekday[1]);
        views.setTextViewText(R.id.widget_5day_day3,weekday[2]);
        views.setTextViewText(R.id.widget_5day_day4,weekday[3]);
        views.setTextViewText(R.id.widget_5day_day5,weekday[4]);

        views.setTextViewText(R.id.widget_5day_temp_max1, StringFormatUtils.formatTemperature(context,weekforecasts.get(0).getMaxTemperature()));
        views.setTextViewText(R.id.widget_5day_temp_max2, StringFormatUtils.formatTemperature(context,weekforecasts.get(1).getMaxTemperature()));
        views.setTextViewText(R.id.widget_5day_temp_max3, StringFormatUtils.formatTemperature(context,weekforecasts.get(2).getMaxTemperature()));
        views.setTextViewText(R.id.widget_5day_temp_max4, StringFormatUtils.formatTemperature(context,weekforecasts.get(3).getMaxTemperature()));
        views.setTextViewText(R.id.widget_5day_temp_max5, StringFormatUtils.formatTemperature(context,weekforecasts.get(4).getMaxTemperature()));

        views.setTextViewText(R.id.widget_5day_temp_min1, StringFormatUtils.formatTemperature(context,weekforecasts.get(0).getMinTemperature()));
        views.setTextViewText(R.id.widget_5day_temp_min2, StringFormatUtils.formatTemperature(context,weekforecasts.get(1).getMinTemperature()));
        views.setTextViewText(R.id.widget_5day_temp_min3, StringFormatUtils.formatTemperature(context,weekforecasts.get(2).getMinTemperature()));
        views.setTextViewText(R.id.widget_5day_temp_min4, StringFormatUtils.formatTemperature(context,weekforecasts.get(3).getMinTemperature()));
        views.setTextViewText(R.id.widget_5day_temp_min5, StringFormatUtils.formatTemperature(context,weekforecasts.get(4).getMinTemperature()));

        views.setImageViewResource(R.id.widget_5day_wind1,StringFormatUtils.colorWindSpeedWidget(weekforecasts.get(0).getWind_speed()));
        views.setImageViewResource(R.id.widget_5day_wind2,StringFormatUtils.colorWindSpeedWidget(weekforecasts.get(1).getWind_speed()));
        views.setImageViewResource(R.id.widget_5day_wind3,StringFormatUtils.colorWindSpeedWidget(weekforecasts.get(2).getWind_speed()));
        views.setImageViewResource(R.id.widget_5day_wind4,StringFormatUtils.colorWindSpeedWidget(weekforecasts.get(3).getWind_speed()));
        views.setImageViewResource(R.id.widget_5day_wind5,StringFormatUtils.colorWindSpeedWidget(weekforecasts.get(4).getWind_speed()));

        Intent intent2 = new Intent(context, ForecastCityActivity.class);
        intent2.putExtra("cityId", getWidgetCityID(context));
        PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent2, 0);
        views.setOnClickPendingIntent(R.id.widget5day_layout, pendingIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {

    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        PFASQLiteHelper dbHelper = PFASQLiteHelper.getInstance(context);

        int widgetCityID=WeatherWidget.getWidgetCityID(context);

        List<Forecast> forecasts=dbHelper.getForecastsByCityId(widgetCityID);
        List<WeekForecast> weekforecasts=dbHelper.getWeekForecastsByCityId(widgetCityID);

        int[] widgetIDs = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, WeatherWidget5day.class));

        for (int widgetID : widgetIDs) {

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.weather_widget_5day);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

            CityToWatch city=dbHelper.getCityToWatch(widgetCityID);

            WeatherWidget5day.updateView(context, appWidgetManager, views, widgetID, city, forecasts,weekforecasts);
            appWidgetManager.updateAppWidget(widgetID, views);

        }
     }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

}

