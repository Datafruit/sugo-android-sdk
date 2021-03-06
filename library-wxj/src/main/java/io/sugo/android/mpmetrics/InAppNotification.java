package io.sugo.android.mpmetrics;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a in-app notification delivered from Mixpanel. Under ordinary circumstances,
 * most code won't have to interact with this class directly, but rather will display
 * InAppNotifications using {@link SugoAPI.People#showNotificationIfAvailable(Activity)}
 * This class is public to
 */
public class InAppNotification implements Parcelable {

    /**
     * InApp Notifications in Mixpanel are either TAKEOVERs, that display full screen,
     * or MINI notifications that appear and disappear on the margins of the screen.
     */
    public enum Type {
        UNKNOWN {
            @Override
            public String toString() {
                return "*unknown_type*";
            }
        },
        MINI {
            @Override
            public String toString() {
                return "mini";
            }
        },
        TAKEOVER {
            @Override
            public String toString() {
                return "takeover";
            }
        };
    }

    public enum Style {
        LIGHT ("light"),
        DARK  ("dark");

        private final String style;

        Style(String s) {
            style = s;
        }

        public boolean equalsName(String otherName) {
            return (otherName != null) && style.equals(otherName);
        }

        public String toString() {
            return this.style;
        }
    }

    public InAppNotification(Parcel in) {
        JSONObject temp = new JSONObject();
        try {
            temp = new JSONObject(in.readString());
        } catch (JSONException e) {
            Log.e(LOGTAG, "Error reading JSON when creating InAppNotification from Parcel");
        }
        mDescription = temp; // mDescription is final
        mId = in.readInt();
        mMessageId = in.readInt();
        mType = in.readString();
        mStyle = in.readString();
        mTitle = in.readString();
        mBody = in.readString();
        mImageUrl = in.readString();
        mCallToAction = in.readString();
        mCallToActionUrl = in.readString();

        mImage = in.readParcelable(Bitmap.class.getClassLoader());
    }

    /* package */ InAppNotification(JSONObject description) throws BadDecideObjectException {
        try {
            mDescription = description;
            mId = description.getInt("id");
            mMessageId = description.getInt("message_id");
            mType = description.getString("type");
            mStyle = description.getString("style");
            mTitle = description.getString("title");
            mBody = description.getString("body");
            mImageUrl = description.getString("image_url");
            mImage = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888);

            // "cta" here is an unfortunate abbreviation of "Call To Action"
            mCallToAction = description.getString("cta");
            mCallToActionUrl = description.getString("cta_url");
        } catch (final JSONException e) {
            throw new BadDecideObjectException("Notification JSON was unexpected or bad", e);
        }
    }

    /* package */ String toJSON() {
        return mDescription.toString();
    }

    /* package */ JSONObject getCampaignProperties() {
        final JSONObject ret = new JSONObject();
        try {
            ret.put("campaign_id", getId());
            ret.put("message_id", getMessageId());
            ret.put("message_type", "inapp");
            ret.put("message_subtype", mType);
        } catch (JSONException e) {
            Log.e(LOGTAG, "Impossible JSON Exception", e);
        }

        return ret;
    }

    public int getId() {
        return mId;
    }

    public int getMessageId() {
        return mMessageId;
    }

    public Type getType() {
        if (Type.MINI.toString().equals(mType)) {
            return Type.MINI;
        }
        if (Type.TAKEOVER.toString().equals(mType)) {
            return Type.TAKEOVER;
        }
        return Type.UNKNOWN;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getBody() {
        return mBody;
    }

    public String getImageUrl() {
        return mImageUrl;
    }

    public String getImage2xUrl() {
        return sizeSuffixUrl(mImageUrl, "@2x");
    }

    public String getImage4xUrl() {
        return sizeSuffixUrl(mImageUrl, "@4x");
    }

    public String getCallToAction() {
        return mCallToAction;
    }

    public String getCallToActionUrl() {
        return mCallToActionUrl;
    }

    public String getStyle() { return mStyle; }

    /* package */ void setImage(final Bitmap image) {
        mImage = image;
    }

    public Bitmap getImage() {
        return mImage;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mDescription.toString());
        dest.writeInt(mId);
        dest.writeInt(mMessageId);
        dest.writeString(mType);
        dest.writeString(mStyle);
        dest.writeString(mTitle);
        dest.writeString(mBody);
        dest.writeString(mImageUrl);
        dest.writeString(mCallToAction);
        dest.writeString(mCallToActionUrl);
        dest.writeParcelable(mImage, flags);
    }

    public static final Creator<InAppNotification> CREATOR = new Creator<InAppNotification>() {

        @Override
        public InAppNotification createFromParcel(Parcel source) {
            return new InAppNotification(source);
        }

        @Override
        public InAppNotification[] newArray(int size) {
            return new InAppNotification[size];
        }
    };

    /* package */ static String sizeSuffixUrl(String url, String sizeSuffix) {
        final Matcher matcher = FILE_EXTENSION_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.replaceFirst(sizeSuffix + "$1");
        } else {
            return url;
        }
    }

    private Bitmap mImage;

    private final JSONObject mDescription;
    private final int mId;
    private final int mMessageId;
    private final String mType;
    private final String mStyle;
    private final String mTitle;
    private final String mBody;
    private final String mImageUrl;
    private final String mCallToAction;
    private final String mCallToActionUrl;

    private static final String LOGTAG = "SugoAPI.InAppNotif";
    private static final Pattern FILE_EXTENSION_PATTERN = Pattern.compile("(\\.[^./]+$)");
}
