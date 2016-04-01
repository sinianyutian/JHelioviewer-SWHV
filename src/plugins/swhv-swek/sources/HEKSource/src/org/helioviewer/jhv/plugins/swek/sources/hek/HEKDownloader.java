package org.helioviewer.jhv.plugins.swek.sources.hek;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.helioviewer.jhv.base.Pair;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKParam;
import org.helioviewer.jhv.database.JHVDatabase;
import org.helioviewer.jhv.database.JHVDatabaseParam;
import org.helioviewer.jhv.plugins.swek.sources.SWEKDownloader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HEKDownloader extends SWEKDownloader {

    @Override
    protected boolean parseEvents(JSONObject eventJSON, JHVEventType type) {
        JSONArray results = eventJSON.getJSONArray("result");
        ArrayList<JHVDatabase.Event2Db> event2db_list = new ArrayList<JHVDatabase.Event2Db>();

        for (int i = 0; i < results.length(); i++) {
            JSONObject result = results.getJSONObject(i);

            String uid = result.getString("kb_archivid");
            long start;
            long end;
            ArrayList<JHVDatabaseParam> paramList = new ArrayList<JHVDatabaseParam>();
            try {
                start = TimeUtils.utcDateFormat.parse(result.getString("event_starttime")).getTime();
                end = TimeUtils.utcDateFormat.parse(result.getString("event_endtime")).getTime();
                HashMap<String, String> dbFields = type.getEventType().getAllDatabaseFields();
                for (Map.Entry<String, String> entry : dbFields.entrySet()) {
                    String dbType = entry.getValue();
                    String fieldName = entry.getKey();
                    String lfieldName = fieldName.toLowerCase();
                    if (dbType.equals(JHVDatabaseParam.DBINTTYPE)) {
                        if (!result.isNull(lfieldName)) {
                            paramList.add(new JHVDatabaseParam(JHVDatabaseParam.DBINTTYPE, result.getInt(lfieldName), fieldName));
                        }
                    } else if (dbType.equals(JHVDatabaseParam.DBSTRINGTYPE)) {
                        if (!result.isNull(lfieldName))
                            paramList.add(new JHVDatabaseParam(JHVDatabaseParam.DBSTRINGTYPE, result.getString(lfieldName), fieldName));
                    }
                }

            } catch (JSONException e) {
                return false;
            } catch (ParseException e) {
                return false;
            }

            if (end - start < 0 || end - start > 3 * 24 * 60 * 60 * 1000) {
                return false;
            }
            byte[] compressedJson;
            try {
                compressedJson = JHVDatabase.compress(result.toString());
            } catch (IOException e) {
                Log.error("compression error");
                return false;
            }
            event2db_list.add(new JHVDatabase.Event2Db(compressedJson, start, end, uid, paramList));

        }
        int id = JHVDatabase.dump_event2db(event2db_list, type);
        if (id == -1) {
            Log.error("failed to dump to database");
            return false;
        }
        return true;
    }

    @Override
    protected boolean parseAssociations(JSONObject eventJSON) {
        JSONArray associations = eventJSON.getJSONArray("association");
        int len = associations.length();
        Pair<String, String>[] assocs = new Pair[len];
        for (int i = 0; i < len; i++) {
            JSONObject asobj = associations.getJSONObject(i);
            assocs[i] = new Pair<String, String>(asobj.getString("first_ivorn"), asobj.getString("second_ivorn"));
        }
        int ret = JHVDatabase.dump_association2db(assocs);
        if (ret == -1) {
            return false;
        }
        return true;
    }

    @Override
    protected String createURL(SWEKEventType eventType, Date startDate, Date endDate, List<SWEKParam> params, int page) {
        StringBuilder baseURL = new StringBuilder(HEKSourceProperties.getSingletonInstance().getHEKSourceProperties().getProperty("heksource.baseurl")).append("?");
        baseURL.append("cmd=search&");
        baseURL.append("type=column&");
        baseURL.append("event_type=").append(HEKEventFactory.getHEKEvent(eventType.getEventName()).getAbbreviation()).append("&");
        baseURL.append("event_coordsys=").append(eventType.getCoordinateSystem()).append("&");
        baseURL.append("x1=").append(eventType.getSpatialRegion().x1).append("&");
        baseURL.append("x2=").append(eventType.getSpatialRegion().x2).append("&");
        baseURL.append("y1=").append(eventType.getSpatialRegion().y1).append("&");
        baseURL.append("y2=").append(eventType.getSpatialRegion().y2).append("&");
        baseURL.append("cosec=2&");
        baseURL.append("param0=event_starttime&op0=<=&value0=").append(TimeUtils.utcDateFormat.format(endDate)).append("&");
        baseURL = appendParams(baseURL, params);
        baseURL.append("event_starttime=").append(TimeUtils.utcDateFormat.format(startDate)).append("&");
        long max = Math.max(System.currentTimeMillis(), endDate.getTime());
        baseURL.append("event_endtime=").append(TimeUtils.utcDateFormat.format(new Date(max))).append("&");
        baseURL.append("page=").append(page);
        return baseURL.toString();
    }

    private static StringBuilder appendParams(StringBuilder baseURL, List<SWEKParam> params) {
        int paramCount = 1;

        for (SWEKParam param : params) {
            if (param.getParam().toLowerCase().equals("provider")) {
                String encodedValue;
                try {
                    encodedValue = URLEncoder.encode(param.getValue(), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    encodedValue = param.getValue();
                }
                baseURL.append("param").append(paramCount).append("=").append("frm_name").append("&").append("op").append(paramCount).append("=").append(param.getOperand().URLEncodedRepresentation()).append("&").append("value").append(paramCount).append("=").append(encodedValue).append("&");
                paramCount++;
            }
        }
        return baseURL;
    }
}
