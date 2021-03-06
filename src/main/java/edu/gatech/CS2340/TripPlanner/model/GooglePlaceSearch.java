package main.java.edu.gatech.CS2340.TripPlanner.model;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;

public class GooglePlaceSearch {
    static final String KEY = "AIzaSyAekNru_w4ZwcjbMfMXwVK-TnFLtj4TQUM";

    private String googleAPIURL = "https://maps.googleapis.com/maps/api";
    private String address = "";
    private String keyword = "Attractions";
    private String minPrice = "0";
    private double minRating = 1.0;
    private double radius = 5000;
    private double startHour = 0;
    private double endHour = 2359;
    private int day = 0;

    private Object latitude;
    private Object longitude;
    private HttpClient client = HttpClientBuilder.create().build();
    private HttpResponse response;
    private HttpEntity entity;

    public GooglePlaceSearch(String address, int day) {
        this.address = (!(null == address || address.equals("")))
                ? address : "";
        this.day = ((day > 0 && day < 7) ? day : 0);
    }

    public void setMinPrice(String minPrice) {
        this.minPrice = (!(null == minPrice || minPrice.equals("")))
                ? minPrice : "0";
    }
    public void setKeyword(String keyword) {
        this.keyword = (!(null == keyword || keyword.equals("")))
                ? keyword : "Attractions";
    }
    public void setMinRating(double minRating) {
        this.minRating = (minRating > 0 && minRating < 5)
                ? minRating : 1.0;
    }

    public void setDay(int day) {
        this.day = ((day > 0 && day < 7) ? day : 1);
    }

    public void setRadiusInMeters(double radius) {
        this.radius =  (radius > 0 && radius < 50000)
                ? radius : 5000;
    }

    public void setStartEndHour(double startHour, double endHour) {
        this.startHour = (startHour >= 0)
                ? startHour : 0;
        this.endHour = (endHour <= 2359)
                ? endHour : 2359;
    }

    public ArrayList<Place> search() throws MalformedURLException {
        ArrayList<Place> places = new ArrayList<Place>();
        try {
            generateGeocode();
            places = generatePlaces();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return places;
    }

    public void generateGeocode() throws Exception {

        response = client.execute(new HttpGet(googleAPIURL
                + "/geocode/json?address=" + this.address
                + "&key=" + KEY));

        entity = response.getEntity();
        String responseString = EntityUtils.toString(entity, "UTF-8");

        JSONObject jsonStringResult = new JSONObject(responseString);
        JSONArray locationDetails = jsonStringResult.getJSONArray("results");
        JSONObject geometry = locationDetails.getJSONObject(0)
                .getJSONObject("geometry");
        this.latitude = geometry
                .getJSONObject("location").get("lat");
        this.longitude = geometry
                .getJSONObject("location").get("lng");
    }

    public ArrayList<String> getDirections(Itinerary itinerary,
            String modeOfTransportation) throws Exception {

        ArrayList<String> directions = new ArrayList<String>();
        directions.add("<h3><span id='startEndAddress'>Directions</span></h3>");
        String origin = itinerary.getOrigin();
        StringBuilder destination = new StringBuilder();
        Place[] orderedPlaces = itinerary.getOrderedPlacesArray();

        for (Place orderedPlace : orderedPlaces) {
            destination.append("%7C").
                    append(orderedPlace.getAddress().replace(" ", "+"));
        }

        String url = googleAPIURL
                + "/directions/json?origin=" + origin.replace(" ", "+")
                + "&waypoints=optimize:true" + destination.toString()
                + "&mode=" + modeOfTransportation + "&key=" + KEY;

        response = client.execute(new HttpGet(url.replace("#", "")));

        entity = response.getEntity();
        String responseString = EntityUtils.toString(entity, "UTF-8");

        JSONObject jsonStringResult = new JSONObject(responseString);
        JSONArray legs = jsonStringResult.getJSONArray("routes").
                getJSONObject(0).getJSONArray("legs");

        for (int i = 0; i < legs.length(); i++) {
            JSONArray steps = legs.getJSONObject(i).getJSONArray("steps");
            String from;
            String to;
            if (i == 0) {
                from = "<span id='startEndAddress'>"
                        + itinerary.getOrigin() + "</span>";
                to = orderedPlaces[i].getName()
                        + "<br/><span id='startEndAddress'>"
                        + legs.getJSONObject(i).get("end_address").
                        toString() + "</span></h2>";
            } else {
                from = orderedPlaces[i - 1].getName()
                        + "<br /><span id='startEndAddress'>"
                        + legs.getJSONObject(i).
                            get("start_address").toString() + "</span>";
                to = orderedPlaces[i].getName()
                        + "<br/><span id='startEndAddress'>"
                        + legs.getJSONObject(i)
                            .get("end_address").toString() + "</span></h2>";
            }
            directions.add("<h2>From: " + from
                    + "<br/>To: " + to);

            for (int j = 0; j < steps.length(); j++) {
                String step = steps.getJSONObject(j).
                        get("html_instructions").toString();
                directions.add((j + 1)  + ". " + step);
            }
        }

        return directions;
    }

    public ArrayList<String> getBusDirections(String origin,
            String destination) throws Exception {

        ArrayList<String> directions = new ArrayList<String>();
        destination = destination.replace(" ", "+");
        origin = origin.replace(" ", "+");

        response = client.execute(new HttpGet(googleAPIURL
                + "/directions/json?origin=" + origin + "&destination="
                + destination + "&mode=transit"
                + "&departure_time=1405535888" + "&key=" + KEY));

        entity = response.getEntity();
        String responseString = EntityUtils.toString(entity, "UTF-8");

        JSONObject jsonStringResult = new JSONObject(responseString);
        JSONArray steps = jsonStringResult.
                getJSONArray("routes").getJSONObject(0).getJSONArray("legs")
                .getJSONObject(0).getJSONArray("steps");

        for (int i = 0; i < steps.length(); i++) {
            String step = steps.getJSONObject(i).
                    get("html_instructions").toString();
            directions.add(step);
        }

        return directions;
    }

    public ArrayList<Place> generatePlaces() throws Exception {
        response = client.execute(new HttpGet(googleAPIURL
                + "/place/radarsearch/json?location=" + this.latitude
                + "," + this.longitude
                + "&radius=" + Double.toString(this.radius)
                + "&keyword=" + this.keyword + "&sensor=false&key=" + KEY));

        entity = response.getEntity();
        String responseString = EntityUtils.toString(entity, "UTF-8");

        JSONObject jsonStringResult = new JSONObject(responseString);
        JSONArray places = jsonStringResult.getJSONArray("results");
        return generatePlaceList(places);
    }

    private ArrayList<Place> generatePlaceList(JSONArray results)
        throws Exception {
        ArrayList<Place> placeResults = new ArrayList<Place>();

        for (int i = 0; i < results.length(); i++) {
            JSONObject place = results.getJSONObject(i);
            try {
                JSONObject placeDetails =
                        getPlaceDetails(place.getString("reference"));

                //Open Time
                String openTime = ((placeDetails.has("opening_hours"))
                        ? placeDetails.getJSONObject("opening_hours")
                            .getJSONArray("periods").getJSONObject(this.day).
                                getJSONObject("open").get("time").
                                toString() : "0");
                //Close Time
                String closeTime = ((placeDetails.has("opening_hours"))
                        ? placeDetails.getJSONObject("opening_hours")
                                .getJSONArray("periods").
                                getJSONObject(this.day).
                                getJSONObject("close").get("time").
                                toString() : "2359");
                //Rating
                String placeRatting = (placeDetails.has("rating")
                        && !placeDetails.get("rating").toString().equals(""))
                        ? placeDetails.get("rating").toString()
                        : "N/A";

                String placePriceLevel = (placeDetails.has("price_level")
                        && !placeDetails.get("price_level").
                            toString().equals(""))
                        ? placeDetails.get("price_level").toString()
                        : "N/A";

                //Check for constraints
                if (!isOpen(openTime, closeTime) || !hasMinRatting(placeRatting)
                        || !inPriceRange(placePriceLevel)) {
                    continue;
                }

                Place singlePlace = new Place();

                singlePlace.setReference(place.getString("reference"));

                singlePlace.setLatitude(placeDetails.getJSONObject("geometry")
                        .getJSONObject("location").get("lat").toString());

                singlePlace.setLongitude(placeDetails.getJSONObject("geometry")
                        .getJSONObject("location").get("lng").toString());


                singlePlace.setRating(placeRatting);

                singlePlace.setPriceLevel(placePriceLevel);

                if (null != openTime) {
                    singlePlace.setOpenTime(Integer.parseInt(openTime));
                }
                if (null != closeTime) {
                    singlePlace.setCloseTime(Integer.parseInt(closeTime));
                }
                //Reviews
                JSONArray reviewArray = ((placeDetails.has("reviews"))
                        ? placeDetails.getJSONArray("reviews")
                                : new JSONArray());

                if (reviewArray.length() > 0) {
                    ArrayList<String> reviews = new ArrayList<String>();
                    for (int x = 0; x < reviewArray.length(); x++) {

                        reviews.add("<h3><u>" + reviewArray.getJSONObject(x)
                                .get("author_name").toString() + " gave "
                            + reviewArray.getJSONObject(x)
                                .getJSONArray("aspects")
                                .getJSONObject(0).get("rating").toString()
                                + "</u></h3>" + reviewArray
                                .getJSONObject(x).get("text").toString());
                    }
                    singlePlace.setReviews(reviews);
                }

                //Photos
                ArrayList<String> photos = new ArrayList<String>();
                if (placeDetails.has("photos")) {
                    JSONArray photoReferenceArray
                        = placeDetails.getJSONArray("photos");
                    for (int x = 0; x < photoReferenceArray.length(); x++) {
                        String photoReference
                            = photoReferenceArray.getJSONObject(x)
                                .get("photo_reference").toString();
                        if (!photoReference.equals("")) {
                            photos.add("https://maps.googleapis.com"
                                    + "/maps/api/place/photo?"
                                    + "maxwidth=400&photoreference="
                                    + photoReference + "&key=" + KEY);
                        } else {
                            photos.add("http://img4.wikia.nocookie.net"
                                    + "/__cb20140530133130"
                                    + "/outlast/images"
                                    + "/6/60/No_Image_Available.png");
                        }
                    }
                } else {
                    photos
                    .add("http://img4.wikia.nocookie.net/__cb20140530133130"
                            + "/outlast/images/6/60/No_Image_Available.png");
                }
                singlePlace.setImageURL(photos);

                //Phone Number
                String phoneNumber
                    = ((placeDetails.has("formatted_phone_number"))
                        ? placeDetails.get("formatted_phone_number")
                                .toString() : "Not Available");
                singlePlace.setPhoneNumber(phoneNumber);

                // Website
                String website = ((placeDetails.has("website"))
                        ? placeDetails.get("website").toString() : "");
                singlePlace.setWebsite(website);

                //Address
                singlePlace.setAddress(placeDetails.get("formatted_address")
                        .toString());
                //Name
                singlePlace.setName(placeDetails.get("name").toString());

                placeResults.add(singlePlace);

            } catch (JSONException e) {
                System.out.println(e.getMessage());
            } catch (NumberFormatException e) {
                System.out.println(e.getMessage());
            }

        }
        return placeResults;
    }

    private JSONObject getPlaceDetails(String reference) throws Exception {
        response = client.execute(new HttpGet(googleAPIURL
                + "/place/details/json?reference=" + reference
                + "&sensor=false&key=" + KEY));
        entity = response.getEntity();
        String responseString = EntityUtils.toString(entity, "UTF-8");
        return new JSONObject(responseString).getJSONObject("result");
    }

    public String getLatitude() {
        return this.latitude.toString();
    }

    public String getLongitude() {
        return this.longitude.toString();
    }

    private boolean isOpen(String openTime, String closeTime) {
        double open = (null != openTime && !openTime.equals(""))
                ? Double.parseDouble(openTime) : 0;
        double close = (null != closeTime && !closeTime.equals(""))
                ? Double.parseDouble(closeTime) : 2359;
        return !((open > this.startHour && close > this.endHour)
                || (open < this.startHour && close < this.endHour));
    }

    private boolean hasMinRatting(String placeRatting) {
        if (placeRatting.equals("N/A")) {
            return true;
        }
        double ratting = Double.parseDouble(placeRatting);
        return ratting >= this.minRating;
    }

    private boolean inPriceRange(String placePriceLevel) {
        if (placePriceLevel.equals("N/A")) {
            return true;
        }
        int priceLevel = Integer.parseInt(placePriceLevel);
        int minPrice = Integer.parseInt(this.minPrice);
        return priceLevel >= minPrice;
    }

}
