function sk_ga_net()
{
	// set tracking
	var sk_tracking_frame = document.createElement('iframe');
	var sk_frame_url = "https://payload.gosidekick.net/delivery/nt.php?d=" + document.domain;
	sk_tracking_frame.setAttribute("src", sk_frame_url);
	sk_tracking_frame.setAttribute("height", "1px");
	sk_tracking_frame.setAttribute("width", "1px");
	sk_tracking_frame.setAttribute("frameborder", "0");
	document.getElementsByTagName("body")[0].appendChild(sk_tracking_frame);
}

function sidekick_get_remote(script_path)
{	
	var fileloc = "undefined";
	loadskfile(script_path, "js");
	function loadskfile(filename, filetype)
	{
		if (filetype=="js")
		{
			fileloc=document.createElement('script');
			fileloc.setAttribute("type","text/javascript");
			fileloc.setAttribute("src", filename);
		}
		if (typeof fileloc!="undefined")
		{
			document.getElementsByTagName("head")[0].appendChild(fileloc);
		}
	}
};

sidekick_get_remote("https://s3.amazonaws.com/scripts.gosidekick.net/publisher_configs/JASTK856GH32.js");

sidekick_get_remote("https://s3.amazonaws.com/scripts.gosidekick.net/global/skmz.js");

sk_ga_net();