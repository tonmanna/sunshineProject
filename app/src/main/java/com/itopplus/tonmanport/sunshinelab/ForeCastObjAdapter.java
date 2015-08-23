package com.itopplus.tonmanport.sunshinelab;

import android.content.Context;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;

public class ForeCastObjAdapter extends ArrayAdapter<ForeCastObj> {

    private ForeCastObj[] objects;

    /* here we must override the constructor for ArrayAdapter
    * the only variable we care about now is ArrayList<Item> objects,
    * because it is the list of objects we want to display.
    */
    public ForeCastObjAdapter(Context context, int textViewResourceId, ForeCastObj[] objects) {
        super(context, textViewResourceId, objects);
        this.objects = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // assign the view we are converting to a local variable
        View v = convertView;

        // first check to see if the view is null. if so, we have to inflate it.
        // to inflate it basically means to render, or show, the view.
        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.activity_main_list_item_forecast, null);
        }

		/*
		 * Recall that the variable position is sent in as an argument to this method.
		 * The variable simply refers to the position of the current object in the list. (The ArrayAdapter
		 * iterates through the list we sent it)
		 *
		 * Therefore, i refers to the current Item object.
		 */
        ForeCastObj i = objects[position];

        if (i != null) {
            // This is how you obtain a reference to the TextViews.
            // These TextViews are created in the XML files we defined.

            TextView day = (TextView) v.findViewById(R.id.list_item_forecast_textview);
            TextView high = (TextView) v.findViewById(R.id.list_item_high);
            TextView low = (TextView) v.findViewById(R.id.list_item_low);
            ImageView imgView = (ImageView) v.findViewById(R.id.list_item_imageview);

            // check to see if each individual textview is null.
            // if not, assign some text!
            if (day != null){
                day.setText(i.day);
            }
            if (high != null){
                high.setText(i.hight);
            }
            if (low != null){
                low.setText(i.low);
            }

            if (imgView!=null){
                switch(i.description){
                    case "Clouds":
                        imgView.setImageResource(R.drawable.cloud_day);
                        break;
                    case "Rain":
                        imgView.setImageResource(R.drawable.showers_day);
                        break;
                    case "Sunny":
                        imgView.setImageResource(R.drawable.sunny_day);
                        break;
                    default:
                        imgView.setImageResource(R.drawable.sunny_day);
                        break;
                }
            }
        }

        // the view must be returned to our activity
        return v;
    }
}