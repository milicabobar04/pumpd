package org.etf.pumpd.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Base64;
import javax.imageio.ImageIO;

import org.springframework.stereotype.Component;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
@Component
public class QRKodGenerator {

	private static final int VELICINA=300;
	private static final int CRNA=0xFF000000;
	private static final int BIJELA=0xFFFFFFFF;
	
	public String kaoDataUrl(String sadrzaj)
	{
		try {
			BitMatrix matrica= new QRCodeWriter().encode(sadrzaj,BarcodeFormat.QR_CODE,VELICINA,VELICINA,Map.of(EncodeHintType.MARGIN,1));
			BufferedImage slika=new BufferedImage(VELICINA,VELICINA,BufferedImage.TYPE_INT_RGB);
			for(int x=0;x<VELICINA;x++)
			{
				for (int y=0;y<VELICINA;y++)
				{
					slika.setRGB(x,y,matrica.get(x,y)?CRNA:BIJELA);
				}
			}
			ByteArrayOutputStream izlaz=new ByteArrayOutputStream();
			ImageIO.write(slika,"png",izlaz);
			return "data:image/png;base64,"+Base64.getEncoder().encodeToString(izlaz.toByteArray());
		}catch(WriterException | IOException e)
		{
			throw new IllegalStateException ("QR kod nije moguce generisati.",e);
		}
	}
}
