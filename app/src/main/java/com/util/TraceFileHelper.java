package com.util;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

public class TraceFileHelper {

    public TraceFileHelper() {

    }

    public static boolean a(String var1, String var2, String var3) {
        a var4 = TraceFileHelper.readTargetDumpInfo(var3, var1, true);
        if (var4 != null && var4.d != null && var4.d.size() > 0) {
            File var5 = new File(var2);
            try {
                if (!var5.exists()) {
                    if (!var5.getParentFile().exists()) {
                        var5.getParentFile().mkdirs();
                    }

                    var5.createNewFile();
                }
            } catch (Exception var21) {
                var21.printStackTrace();
                Timber.e("backup file create error! %s  %s", var21.getClass().getName() + ":" + var21.getMessage(), var2);
                return false;
            }

            if (var5.exists() && var5.canWrite()) {
                BufferedWriter var6 = null;

                boolean var8;
                try {
                    var6 = new BufferedWriter(new FileWriter(var5, false));
                    String[] var7 = (String[])var4.d.get("main");
                    String var10;
                    if (var7 != null && var7.length >= 3) {
                        String var25 = var7[0];
                        String var9 = var7[1];
                        var10 = var7[2];
                        var6.write("\"main\" tid=" + var10 + " :\n" + var25 + "\n" + var9 + "\n\n");
                        var6.flush();
                    }

                    Iterator var26 = var4.d.entrySet().iterator();

                    while(var26.hasNext()) {
                        Map.Entry var27 = (Map.Entry)var26.next();
                        if (!((String)var27.getKey()).equals("main") && var27.getValue() != null && ((String[])var27.getValue()).length >= 3) {
                            var10 = ((String[])var27.getValue())[0];
                            String var11 = ((String[])var27.getValue())[1];
                            String var12 = ((String[])var27.getValue())[2];
                            var6.write("\"" + (String)var27.getKey() + "\" tid=" + var12 + " :\n" + var10 + "\n" + var11 + "\n\n");
                            var6.flush();
                        }
                    }

                    var8 = true;
                    return var8;
                } catch (IOException var23) {
                    var23.printStackTrace();
                    Timber.e("dump trace fail %s",var23.getClass().getName() + ":" + var23.getMessage());
                    var8 = false;
                } finally {
                    if (var6 != null) {
                        try {
                            var6.close();
                        } catch (IOException var22) {
                            var22.printStackTrace();
                        }
                    }

                }

                return var8;
            } else {
                Timber.e("backup file create fail %s", var2);
                return false;
            }
        } else {
            Timber.e("not found trace dump for %s", var3);
            return false;
        }
    }

    public static TraceFileHelper.a readTargetDumpInfo(final String targetProcName, String traceFilePath, final boolean needThread) {
        if (targetProcName != null && traceFilePath != null) {
            final TraceFileHelper.a var3 = new TraceFileHelper.a();
            readTraceFile(traceFilePath, new TraceFileHelper.b() {
                public boolean a(String var1, long var2, long var4) {
                    return true;
                }

                public boolean a(String var1, int var2, String var3x, String var4, boolean var5) {
                    Timber.d("new thread %s", new Object[]{var1});
                    if (var3.a > 0L && var3.c > 0L && var3.b != null) {
                        if (var3.d == null) {
                            var3.d = new HashMap();
                        }

                        var3.d.put(var1, new String[]{var3x, var4, "" + var2});
                        return true;
                    } else {
                        return true;
                    }
                }

                public boolean a(long var1, long var3x, String var5) {
                    Timber.d("new process %s", new Object[]{var5});
                    if (!var5.equals(targetProcName)) {
                        return true;
                    } else {
                        var3.a = var1;
                        var3.c = var3x;
                        return needThread;
                    }
                }

                public boolean a(long var1) {
                    Timber.d("process end %d", var1);
                    return var3.a <= 0L || var3.c <= 0L || var3.b == null;
                }
            });
            return var3/*.a > 0L && var3.c > 0L && var3.d != null ? var3 : null*/;
        } else {
            return null;
        }
    }

    public static TraceFileHelper.a readFirstDumpInfo(String traceFilePath, final boolean needThread) {
        if (traceFilePath == null) {
            Timber.e("path:%s", traceFilePath);
            return null;
        } else {
            final TraceFileHelper.a var2 = new TraceFileHelper.a();
            readTraceFile(traceFilePath, new TraceFileHelper.b() {
                public boolean a(String var1, long var2x, long var4) {
                    return true;
                }

                public boolean a(String var1, int var2x, String var3, String var4, boolean var5) {
                    Timber.d("new thread %s", new Object[]{var1});
                    if (var2.d == null) {
                        var2.d = new HashMap();
                    }

                    var2.d.put(var1, new String[]{var3, var4, "" + var2x});
                    return true;
                }

                public boolean a(long var1, long var3, String var5) {
                    Timber.d("new process %s", new Object[]{var5});
                    var2.a = var1;
                    var2.b = var5;
                    var2.c = var3;
                    return needThread;
                }

                public boolean a(long var1) {
                    Timber.d("process end %d", var1);
                    return false;
                }
            });
            if (var2.a > 0L && var2.c > 0L && var2.b != null) {
                return var2;
            } else {
                Timber.e("first dump error %s",(var2.a + " " + var2.c + " " + var2.b));
                return null;
            }
        }
    }

    public static void readTraceFile(String traceFilePath, TraceFileHelper.b visitor) {
        if (traceFilePath != null && visitor != null) {
            File var2 = new File(traceFilePath);
            if (var2.exists()) {
                if (visitor.a(traceFilePath, var2.lastModified(), var2.length())) {
                    BufferedReader var3 = null;

                    try {
                        var3 = new BufferedReader(new FileReader(var2));
                        Object[] var4 = null;
                        Pattern var5 = Pattern.compile("-{5}\\spid\\s\\d+\\sat\\s\\d+-\\d+-\\d+\\s\\d{2}:\\d{2}:\\d{2}\\s-{5}");
                        Pattern var6 = Pattern.compile("-{5}\\send\\s\\d+\\s-{5}");
                        Pattern var7 = Pattern.compile("Cmd\\sline:\\s(\\S+)");
                        Pattern var8 = Pattern.compile("\".+\"\\s(daemon\\s){0,1}prio=\\d+\\stid=\\d+\\s.*");
                        SimpleDateFormat var9 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

                        while ((var4 = a(var3, var5)) != null) {
                            String[] var10 = var4[1].toString().split("\\s");
                            long var11 = Long.parseLong(var10[2]);
                            String var13 = var10[4] + " " + var10[5];
                            Date var14 = var9.parse(var13);
                            long var15 = var14.getTime();
                            if ((var4 = a(var3, var7)) == null) {
                                return;
                            }

                            Matcher var17 = var7.matcher(var4[1].toString());
                            var17.find();
                            var17.group(1);
                            String var18 = var17.group(1);
                            if (!visitor.a(var11, var15, var18)) {
                                return;
                            }

                            while ((var4 = a(var3, var8, var6)) != null) {
                                if (var4[0] != var8) {
                                    var10 = var4[1].toString().split("\\s");
                                    var11 = Long.parseLong(var10[2]);
                                    if (!visitor.a(var11)) {
                                        return;
                                    }
                                    break;
                                }

                                String var19 = var4[1].toString();
                                Pattern var20 = Pattern.compile("\".+\"");
                                Matcher var21 = var20.matcher(var19);
                                var21.find();
                                String var22 = var21.group();
                                var22 = var22.substring(1, var22.length() - 1);
                                boolean var23 = var19.contains("NATIVE");
                                Pattern var24 = Pattern.compile("tid=\\d+");
                                Matcher var25 = var24.matcher(var19);
                                var25.find();
                                String var26 = var25.group();
                                var26 = var26.substring(var26.indexOf("=") + 1);
                                int var27 = Integer.parseInt(var26);
                                String var28 = a(var3);
                                String var29 = b(var3);
                                if (!visitor.a(var22, var27, var28, var29, var23)) {
                                    return;
                                }
                            }
                        }

                    } catch (Exception var43) {
                        Timber.e("trace open fail:%s : %s", var43.getClass().getName(), var43.getMessage());
                    } finally {
                        if (var3 != null) {
                            try {
                                var3.close();
                            } catch (IOException var42) {
                                var42.printStackTrace();
                            }
                        }

                    }
                }
            }
        }
    }

    protected static Object[] a(BufferedReader var0, Pattern... var1) throws IOException {
        if (var0 != null && var1 != null) {
            String var2 = null;

            while ((var2 = var0.readLine()) != null) {
                Pattern[] var3 = var1;
                int var4 = var1.length;

                for (int var5 = 0; var5 < var4; ++var5) {
                    Pattern var6 = var3[var5];
                    Matcher var7 = var6.matcher(var2);
                    if (var7.matches()) {
                        Object[] var8 = new Object[]{var6, var2};
                        return var8;
                    }
                }
            }

            return null;
        } else {
            return null;
        }
    }

    protected static String a(BufferedReader var0) throws IOException {
        StringBuffer var1 = new StringBuffer();
        String var2 = null;

        for (int var3 = 0; var3 < 3; ++var3) {
            var2 = var0.readLine();
            if (var2 == null) {
                return null;
            }

            var1.append(var2 + "\n");
        }

        return var1.toString();
    }

    protected static String b(BufferedReader var0) throws IOException {
        StringBuffer var1 = new StringBuffer();
        String var2 = null;

        while ((var2 = var0.readLine()) != null && var2.trim().length() > 0) {
            var1.append(var2 + "\n");
        }

        return var1.toString();
    }

    public static class a {
        public long a;
        public String b;
        public long c;
        public Map<String, String[]> d;

        public a() {
        }
    }

    public interface b {
        boolean a(String var1, long var2, long var4);

        boolean a(long var1, long var3, String var5);

        boolean a(long var1);

        boolean a(String var1, int var2, String var3, String var4, boolean var5);
    }
}

