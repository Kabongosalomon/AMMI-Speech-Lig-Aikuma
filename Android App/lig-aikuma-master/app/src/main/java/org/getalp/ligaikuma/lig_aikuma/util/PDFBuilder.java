package org.getalp.ligaikuma.lig_aikuma.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.format.DateFormat;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.VerticalPositionMark;

import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Date;

public class PDFBuilder
{
    private static final String TAG = "PDFBuilder";

    private File _pdfFile;
    private Context _context;
    private Document _document = null;

    public PDFBuilder(Context context)
    {
        _context = context;
    }

    /** Build a new consent form for speaking
     *
     * @param PDFFolder Folder where make PDF file
     * @param PDFName PDF filename, with or not .pdf
     * @param userName Name of skeaker
     * @return
     */
    public PDFBuilder BuildConsentForm(String PDFFolder, String PDFName, String userName)
    {
        try {
            initBuild(PDFFolder, PDFName.replace(".pdf","")+".pdf");

            String[] consent = _context.getString(R.string.consent_pdf).split("§!§");

            buildTitle(consent[0]);
            addParagraph(consent[1]+userName+ consent[2]);
            addParagraph(consent[3]);
            addParagraph(consent[4]);
            addParagraph(consent[5]);
            addParagraph(consent[6]);
            buildParallelSentence(consent[7]+userName, consent[8]);
            buildLineJump(5);
            String t = consent[9]+DateFormat.format("dd/MM/yyyy", new Date().getTime())+consent[10];
            buildParallelSentence(t, t);
            buildLineJump(5);
            buildParallelSentence(consent[11], consent[11]);
            _document.close();
        } catch (IOException | DocumentException ignored) { }
        return this;
    }

    /** Use an external application to view the PDF
     *
     * @param c Context used to start the new application
     * @param pdfFile Pdf file to open
     */
    public static void previewPdf(Context c, String pdfFile)
    {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new File(pdfFile)), "application/pdf");
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        c.startActivity(intent);
    }

     /** Move and rename your file
     *
     * @param source File to move. Use: .../repo/fileSour.pdf
     * @param destination Target to move source file. Use: ...repo2/fileDest.pdf
     * @return True if the move is successful, false otherwise
     */
    public static boolean moveAndRenameFile(String source, String destination)
    {
        if(!new File(source).exists())    return false;
        try
        {
            FileChannel inChannel = new FileInputStream(source).getChannel(),
                    outChannel = new FileOutputStream(destination).getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
            inChannel.close();
            outChannel.close();
            return new File(source).delete();
        }
        catch(IOException ignored){}
        return false;
    }

    //          ######################################
    //          #               BUILDER              #
    //          ######################################

    /** Build new title
     *
     * @param title Content of title
     * @throws IOException
     * @throws DocumentException
     */
    private void buildTitle(String title) throws IOException, DocumentException
    {
        Paragraph p = new Paragraph(title, new Font(Font.FontFamily.TIMES_ROMAN, 18, Font.BOLD));
        p.setAlignment(Element.ALIGN_CENTER);
        _document.add(p);
        _document.add(new Paragraph(" "));
        _document.add(new Paragraph(" "));
    }

    /** Build new paragraph
     *
     * @param paragraph Content of
     * @throws DocumentException
     */
    private void addParagraph(String paragraph) throws DocumentException
    {
        Paragraph p = new Paragraph(paragraph);
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        _document.add(p);
        _document.add(new Paragraph(" "));
    }

    /** Build 2 sentences, on right and left alignment
     *
     * @param left Content of left sentence
     * @param right Content of right sentence
     * @throws DocumentException
     */
    private void buildParallelSentence(String left, String right) throws DocumentException
    {
        Paragraph p = new Paragraph(left);
        p.add(new Chunk(new VerticalPositionMark()));
        p.add(right+"    ");
        _document.add(p);
    }

    /** Jump n line
     *
     * @param n Number of line to fump
     * @throws DocumentException
     */
    private void buildLineJump(int n) throws DocumentException
    {
        for(int i=0;i<n;i++)
            _document.add(new Paragraph(" "));
    }

    /** Initialize the PDF file
     *
     * @param PDFFolder Folder where make PDF file
     * @param PDFName PDF filename, don't forget finish by .pdf
     * @throws FileNotFoundException
     * @throws DocumentException
     */
    private void initBuild(String PDFFolder, String PDFName) throws FileNotFoundException, DocumentException
    {
        File pdfFolder = new File(PDFFolder);
        if(!pdfFolder.exists()) pdfFolder.mkdir();
        _pdfFile = new File(PDFFolder + PDFName);
        _document = new Document();
        PdfWriter.getInstance(_document, new FileOutputStream(_pdfFile));
        _document.open();
    }
}


//          =,    (\_/)    ,=
//           /`-'--(")--'-'\
//          /     (___)     \
//         /.-.-./ " " \.-.-.\