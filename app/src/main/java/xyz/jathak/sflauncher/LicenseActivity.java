package xyz.jathak.sflauncher;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;


public class LicenseActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license);
        TextView indexLabel = (TextView)findViewById(R.id.indexable_label);
        indexLabel.setMovementMethod(LinkMovementMethod.getInstance());
        TextView indexLicense = (TextView)findViewById(R.id.indexable_license);
        TextView swipeLabel = (TextView)findViewById(R.id.swipe_label);
        swipeLabel.setMovementMethod(LinkMovementMethod.getInstance());
        TextView swipeLicense = (TextView)findViewById(R.id.swipe_license);
        TextView sfLabel = (TextView)findViewById(R.id.sflauncher_label);
        sfLabel.setMovementMethod(LinkMovementMethod.getInstance());
        TextView sfLicense = (TextView)findViewById(R.id.sflauncher_license);
        indexLabel.setText(Html.fromHtml("<a href='https://github.com/woozzu/IndexableListView'>IndexableListView</a>"));
        swipeLabel.setText(Html.fromHtml("<a href='https://github.com/daimajia/AndroidSwipeLayout'>Android Swipe Layout</a>"));
        sfLabel.setText(Html.fromHtml("<a href='https://github.com/jathak/sflauncher'>SF Launcher 2</a>"));
        indexLicense.setText("Copyright 2011 woozzu\n" +
                "\n" +
                "Licensed under the Apache License, Version 2.0 (the \"License\"); you may not use this file except in compliance with the License. You may obtain a copy of the License at\n" +
                "\n" +
                "http://www.apache.org/licenses/LICENSE-2.0\n" +
                "Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.");
        swipeLicense.setText("The MIT License (MIT)\n" +
                "\n" +
                "Copyright (c) 2014 代码家\n" +
                "\n" +
                "Permission is hereby granted, free of charge, to any person obtaining a copy\n" +
                "of this software and associated documentation files (the \"Software\"), to deal\n" +
                "in the Software without restriction, including without limitation the rights\n" +
                "to use, copy, modify, merge, publish, distribute, sublicense, and/or sell\n" +
                "copies of the Software, and to permit persons to whom the Software is\n" +
                "furnished to do so, subject to the following conditions:\n" +
                "\n" +
                "The above copyright notice and this permission notice shall be included in all\n" +
                "copies or substantial portions of the Software.\n" +
                "\n" +
                "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR\n" +
                "IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,\n" +
                "FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE\n" +
                "AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER\n" +
                "LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,\n" +
                "OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE\n" +
                "SOFTWARE.");
        sfLicense.setText("Copyright (c) 2015-2017, J.A. Thakar\n" +
                "All rights reserved.\n" +
                "\n" +
                "Redistribution and use in source and binary forms, with or without\n" +
                "modification, are permitted provided that the following conditions are met:\n" +
                "    * Redistributions of source code must retain the above copyright\n" +
                "      notice, this list of conditions and the following disclaimer.\n" +
                "    * Redistributions in binary form must reproduce the above copyright\n" +
                "      notice, this list of conditions and the following disclaimer in the\n" +
                "      documentation and/or other materials provided with the distribution.\n" +
                "    * Neither the name of the author nor the\n" +
                "      names of its contributors may be used to endorse or promote products\n" +
                "      derived from this software without specific prior written permission.\n" +
                "\n" +
                "THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\" AND\n" +
                "ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED\n" +
                "WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE\n" +
                "DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY\n" +
                "DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES\n" +
                "(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;\n" +
                "LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND\n" +
                "ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT\n" +
                "(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS\n" +
                "SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.");
    }
}
